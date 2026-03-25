package com.mindflow.security.search;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransitStopRepository extends JpaRepository<TransitStopEntity, Long> {
    Optional<TransitStopEntity> findTopByStopNameIgnoreCase(String stopName);
}
