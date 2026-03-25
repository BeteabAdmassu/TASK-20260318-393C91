package com.mindflow.security.admin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldDictionaryRepository extends JpaRepository<FieldDictionaryEntity, Long> {
    List<FieldDictionaryEntity> findByCategoryOrderByCodeAsc(String category);
}
