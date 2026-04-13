package com.egs.rjsf.repository;

import com.egs.rjsf.entity.DocumentCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentCatalogRepository extends JpaRepository<DocumentCatalog, UUID> {

    List<DocumentCatalog> findByIsActiveTrueOrderByCategoryAscFileNameAsc();

    List<DocumentCatalog> findByCategoryAndIsActiveTrueOrderByFileNameAsc(String category);
}
