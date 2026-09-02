package com.sih26190.dms.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.sih26190.dms.blockchain.BlockchainService;
import com.sih26190.dms.dto.DocumentResponse;
import com.sih26190.dms.dto.UpdateDocumentRequest;
import com.sih26190.dms.dto.VerifyResponse;
import com.sih26190.dms.model.DocumentRecord;
import com.sih26190.dms.model.Role;
import com.sih26190.dms.model.User;
import com.sih26190.dms.repository.DocumentRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRecordRepository documentRecordRepository;
    private final AuditService auditService;
    private final BlockchainService blockchainService;

    @Value("${dms.storage.location}")
    private String storageLocation;

    /**
     * Saves the file to disk, saves the metadata row, writes an audit
     * log entry, and anchors the file's hash on-chain via
     * BlockchainService, all treated as one unit: if the blockchain
     * call fails, the whole upload fails, rather than silently leaving
     * a document with no tamper-evidence record. Given the transaction
     * is @Transactional, a thrown exception here rolls back the DB
     * writes too.
     */
    @Transactional
    public DocumentResponse upload(MultipartFile file, String caseId, String documentType, User uploader) {
        try {
            byte[] fileBytes = file.getBytes();

            Path directory = Paths.get(storageLocation);
            Files.createDirectories(directory);

            String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path destination = directory.resolve(storedFileName);
            Files.write(destination, fileBytes);

            DocumentRecord document = new DocumentRecord();
            document.setCaseId(caseId);
            document.setDocumentType(documentType);
            document.setFilePath(destination.toString());
            document.setOriginalFileName(file.getOriginalFilename());
            document.setUploadedBy(uploader);
            document.setUploadedAt(LocalDateTime.now());

            DocumentRecord saved = documentRecordRepository.save(document);

            auditService.log(uploader, saved, "UPLOAD");

            try {
                blockchainService.storeDocumentHash(saved.getId(), fileBytes, caseId);
            } catch (Exception e) {
                throw new RuntimeException("Failed to anchor document hash on-chain, upload rolled back", e);
            }

            return toResponse(saved);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store uploaded file", e);
        }
    }

    /**
     * OFFICER sees only their own uploads. ADMIN sees everything.
     */
    public List<DocumentResponse> listForUser(User requester) {
        List<DocumentRecord> records = requester.getRole() == Role.ADMIN
                ? documentRecordRepository.findAll()
                : documentRecordRepository.findByUploadedBy(requester);

        return records.stream().map(this::toResponse).toList();
    }

    /**
     * Enforces per-document access before returning it, and logs the view
     * regardless of whether access is granted or denied.
     */
    @Transactional
    public DocumentResponse getById(Long id, User requester) {
        DocumentRecord document = documentRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        requireOwnerOrAdmin(document, requester);

        auditService.log(requester, document, "VIEW");

        return toResponse(document);
    }

    /**
     * Recomputes the on-disk file's hash right now and compares it
     * against what was anchored on-chain at upload time. A mismatch
     * means the file was altered outside the system after upload.
     * Logs a VERIFY action either way.
     */
    @Transactional
    public VerifyResponse verify(Long id, User requester) {
        DocumentRecord document = documentRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        requireOwnerOrAdmin(document, requester);

        byte[] currentBytes;
        try {
            currentBytes = Files.readAllBytes(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Could not read file from disk to verify it", e);
        }

        boolean matches;
        try {
            matches = blockchainService.verifyDocumentHash(id, currentBytes);
        } catch (Exception e) {
            throw new RuntimeException("Could not reach the blockchain to verify this document", e);
        }

        auditService.log(requester, document, "VERIFY");

        String message = matches
                ? "File matches the hash recorded on-chain at upload time."
                : "File does NOT match the on-chain record, it may have been altered since upload.";

        return new VerifyResponse(id, !matches, message);
    }

    /**
     * Updates metadata only (caseId, documentType), not the file itself.
     * Same ownership rule as view/delete: owner or ADMIN. Logs UPDATE.
     */
    @Transactional
    public DocumentResponse update(Long id, UpdateDocumentRequest request, User requester) {
        DocumentRecord document = documentRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        requireOwnerOrAdmin(document, requester);

        if (request.getCaseId() != null && !request.getCaseId().isBlank()) {
            document.setCaseId(request.getCaseId());
        }
        if (request.getDocumentType() != null && !request.getDocumentType().isBlank()) {
            document.setDocumentType(request.getDocumentType());
        }

        DocumentRecord saved = documentRecordRepository.save(document);
        auditService.log(requester, saved, "UPDATE");

        return toResponse(saved);
    }

    /**
     * Deletes the metadata row and the underlying file. The audit log
     * entry is written first, capturing the document's id and caseId as
     * plain values (see AuditLog), so the audit trail survives even
     * though the DocumentRecord row itself is removed right after.
     * The on-chain hash record is intentionally left in place, since
     * the blockchain layer is append-only by design, this is itself a
     * useful property: even a deleted document's integrity history is
     * still verifiable if the file is recovered from a backup later.
     */
    @Transactional
    public void delete(Long id, User requester) {
        DocumentRecord document = documentRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        requireOwnerOrAdmin(document, requester);

        auditService.log(requester, document, "DELETE");

        try {
            Files.deleteIfExists(Paths.get(document.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from disk", e);
        }

        documentRecordRepository.delete(document);
    }

    private void requireOwnerOrAdmin(DocumentRecord document, User requester) {
        boolean isOwner = document.getUploadedBy().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have access to this document");
        }
    }

    private DocumentResponse toResponse(DocumentRecord document) {
        return new DocumentResponse(
                document.getId(),
                document.getCaseId(),
                document.getDocumentType(),
                document.getOriginalFileName(),
                document.getUploadedBy().getUsername(),
                document.getUploadedAt()
        );
    }

}