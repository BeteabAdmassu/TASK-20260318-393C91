package com.mindflow.security.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, Long> {
    Optional<SystemConfigEntity> findByConfigKey(String configKey);
}
