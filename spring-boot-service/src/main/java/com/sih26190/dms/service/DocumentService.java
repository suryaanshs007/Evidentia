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


    public List<DocumentResponse> listForUser(User requester) {
        List<DocumentRecord> records = requester.getRole() == Role.ADMIN
                ? documentRecordRepository.findAll()
                : documentRecordRepository.findByUploadedBy(requester);

        return records.stream().map(this::toResponse).toList();
    }

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
