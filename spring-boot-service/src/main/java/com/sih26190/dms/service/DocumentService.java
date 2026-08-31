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

import com.sih26190.dms.dto.DocumentResponse;
import com.sih26190.dms.model.DocumentRecord;
import com.sih26190.dms.model.Role;
import com.sih26190.dms.model.User;
import com.sih26190.dms.repository.DocumentRecordRepository;
import com.sih26190.dms.dto.UpdateDocumentRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRecordRepository documentRecordRepository;
    private final AuditService auditService;

    @Value("${dms.storage.location}")
    private String storageLocation;


    @Transactional
    public DocumentResponse upload(MultipartFile file, String caseId, String documentType, User uploader) {
        try {
            Path directory = Paths.get(storageLocation);
            Files.createDirectories(directory);

            String storedFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path destination = directory.resolve(storedFileName);
            file.transferTo(destination);

            DocumentRecord document = new DocumentRecord();
            document.setCaseId(caseId);
            document.setDocumentType(documentType);
            document.setFilePath(destination.toString());
            document.setOriginalFileName(file.getOriginalFilename());
            document.setUploadedBy(uploader);
            document.setUploadedAt(LocalDateTime.now());

            DocumentRecord saved = documentRecordRepository.save(document);

            auditService.log(uploader, saved, "UPLOAD");

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

        boolean isOwner = document.getUploadedBy().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have access to this document");
        }

        auditService.log(requester, document, "VIEW");

        return toResponse(document);
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
