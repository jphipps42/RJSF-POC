package com.egs.rjsf.controller;

import com.egs.rjsf.entity.DocumentCatalog;
import com.egs.rjsf.repository.DocumentCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/document-catalog")
@RequiredArgsConstructor
public class DocumentCatalogController {

    private final DocumentCatalogRepository repository;

    @GetMapping
    public ResponseEntity<List<DocumentCatalog>> list(
            @RequestParam(name = "category", required = false) String category) {
        List<DocumentCatalog> docs;
        if (category != null && !category.isBlank()) {
            docs = repository.findByCategoryAndIsActiveTrueOrderByFileNameAsc(category);
        } else {
            docs = repository.findByIsActiveTrueOrderByCategoryAscFileNameAsc();
        }
        return ResponseEntity.ok(docs);
    }
}
