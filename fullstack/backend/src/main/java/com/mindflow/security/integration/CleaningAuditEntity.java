package com.mindflow.security.integration;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "cleaning_audit_logs")
public class CleaningAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "import_job_id", nullable = false)
    private Long importJobId;

    @Column(name = "source_ref", nullable = false, length = 255)
    private String sourceRef;

    @Column(name = "field_name", nullable = false, length = 120)
    private String fieldName;

    @Column(name = "raw_value", nullable = false, length = 1000)
    private String rawValue;

    @Column(name = "cleaned_value", nullable = false, length = 1000)
    private String cleanedValue;

    @Column(name = "action", nullable = false, length = 120)
    private String action;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getImportJobId() {
        return importJobId;
    }

    public void setImportJobId(Long importJobId) {
        this.importJobId = importJobId;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }

    public String getCleanedValue() {
        return cleanedValue;
    }

    public void setCleanedValue(String cleanedValue) {
        this.cleanedValue = cleanedValue;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
