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
@Table(name = "stop_structure_versions")
public class StopStructureVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stop_name", nullable = false, length = 200)
    private String stopName;

    @Column(name = "field_name", nullable = false, length = 120)
    private String fieldName;

    @Column(name = "old_value", nullable = false, length = 1000)
    private String oldValue;

    @Column(name = "new_value", nullable = false, length = 1000)
    private String newValue;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "import_job_id", nullable = false)
    private Long importJobId;

    @PrePersist
    public void prePersist() {
        this.changedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Long getImportJobId() {
        return importJobId;
    }

    public void setImportJobId(Long importJobId) {
        this.importJobId = importJobId;
    }
}
