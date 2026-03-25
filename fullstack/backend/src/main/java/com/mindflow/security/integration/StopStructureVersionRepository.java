package com.mindflow.security.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StopStructureVersionRepository extends JpaRepository<StopStructureVersionEntity, Long> {
    Optional<StopStructureVersionEntity> findTopByStopNameAndFieldNameOrderByVersionNumberDesc(String stopName, String fieldName);

    List<StopStructureVersionEntity> findByStopNameOrderByVersionNumberDesc(String stopName);

    List<StopStructureVersionEntity> findByImportJobIdOrderByIdAsc(Long importJobId);
}
