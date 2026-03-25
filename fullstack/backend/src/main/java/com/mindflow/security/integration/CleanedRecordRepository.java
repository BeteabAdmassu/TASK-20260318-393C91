package com.mindflow.security.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CleanedRecordRepository extends JpaRepository<CleanedRecordEntity, Long> {
    Optional<CleanedRecordEntity> findTopByStopNameOrderByIdDesc(String stopName);
}
