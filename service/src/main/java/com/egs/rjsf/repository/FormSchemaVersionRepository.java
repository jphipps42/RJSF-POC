package com.egs.rjsf.repository;

import com.egs.rjsf.entity.FormSchemaVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormSchemaVersionRepository extends JpaRepository<FormSchemaVersion, UUID> {

    List<FormSchemaVersion> findByFormIdOrderByVersionDesc(UUID formId);

    Optional<FormSchemaVersion> findByFormIdAndIsCurrentTrue(UUID formId);

    Optional<FormSchemaVersion> findByFormIdAndVersion(UUID formId, Integer version);
}
