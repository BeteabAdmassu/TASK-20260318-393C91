package com.mindflow.security.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CleaningAuditRepository extends JpaRepository<CleaningAuditEntity, Long> {
    List<CleaningAuditEntity> findByImportJobIdOrderByIdAsc(Long importJobId);
}
