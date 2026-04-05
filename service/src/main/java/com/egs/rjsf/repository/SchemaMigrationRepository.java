package com.egs.rjsf.repository;

import com.egs.rjsf.entity.SchemaMigration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SchemaMigrationRepository extends JpaRepository<SchemaMigration, UUID> {

    List<SchemaMigration> findByFormIdAndFromVersionGreaterThanEqualAndFromVersionLessThanOrderByFromVersionAsc(
            UUID formId, Integer fromVersionStart, Integer fromVersionEnd);
}
