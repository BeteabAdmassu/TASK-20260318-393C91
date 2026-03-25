package com.mindflow.security.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FieldDictionaryRepository extends JpaRepository<FieldDictionaryEntity, Long> {
    List<FieldDictionaryEntity> findByCategoryOrderByCodeAsc(String category);

    List<FieldDictionaryEntity> findByCategoryAndEnabledTrueOrderByCodeAsc(String category);

    Optional<FieldDictionaryEntity> findByCategoryAndCodeAndEnabledTrue(String category, String code);
}
