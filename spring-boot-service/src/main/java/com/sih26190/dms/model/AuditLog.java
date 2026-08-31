package com.sih26190.dms.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // documentId and caseId are stored as plain values captured at the
    // time of the action, not a JPA relationship to DocumentRecord.
    // An audit log must survive deletion of the thing it is auditing,
    // a foreign key here would either block deleting a document or
    // silently orphan/cascade-delete its own history, both wrong for
    // an audit trail.
    private Long documentId;

    private String caseId;

    // e.g. UPLOAD, VIEW, UPDATE, DELETE
    private String action;

    private LocalDateTime timestamp;

}
