package com.egs.rjsf.transformer.repository;

import com.egs.rjsf.transformer.entity.TemplateHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateHistoryRepository extends JpaRepository<TemplateHistoryEntity, Long> {
    Optional<TemplateHistoryEntity> findByFormIdAndVersion(String formId, Integer version);
    Optional<TemplateHistoryEntity> findTopByFormIdOrderByVersionDesc(String formId);
}
