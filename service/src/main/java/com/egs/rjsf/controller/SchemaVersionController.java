package com.egs.rjsf.controller;

import com.egs.rjsf.dto.PublishVersionRequest;
import com.egs.rjsf.entity.FormSchemaVersion;
import com.egs.rjsf.service.SchemaVersionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/schema-versions")
@CrossOrigin
public class SchemaVersionController {

    private final SchemaVersionService schemaVersionService;

    public SchemaVersionController(SchemaVersionService schemaVersionService) {
        this.schemaVersionService = schemaVersionService;
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<FormSchemaVersion>> getAllVersions(@PathVariable UUID formId) {
        return ResponseEntity.ok(schemaVersionService.findAllByFormId(formId));
    }

    @GetMapping("/{formId}/current")
    public ResponseEntity<FormSchemaVersion> getCurrentVersion(@PathVariable UUID formId) {
        return ResponseEntity.ok(schemaVersionService.findCurrentByFormId(formId));
    }

    @GetMapping("/{formId}/{version}")
    public ResponseEntity<FormSchemaVersion> getByVersion(@PathVariable UUID formId,
                                                          @PathVariable Integer version) {
        return ResponseEntity.ok(schemaVersionService.findByFormIdAndVersion(formId, version));
    }

    @PostMapping("/{formId}/publish")
    public ResponseEntity<FormSchemaVersion> publish(@PathVariable UUID formId,
                                                     @RequestBody PublishVersionRequest request) {
        FormSchemaVersion published = schemaVersionService.publish(
                formId,
                request.json_schema(),
                request.ui_schema(),
                request.default_data(),
                request.change_notes(),
                request.migration_rules()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(published);
    }

    @PutMapping("/{formId}/{version}/set-current")
    public ResponseEntity<FormSchemaVersion> setCurrent(@PathVariable UUID formId,
                                                        @PathVariable Integer version) {
        return ResponseEntity.ok(schemaVersionService.setCurrent(formId, version));
    }
}
