package com.mindflow.security.monitoring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertDiagnosticRepository extends JpaRepository<AlertDiagnosticEntity, Long> {
    List<AlertDiagnosticEntity> findTop100ByOrderByCreatedAtDesc();
}
