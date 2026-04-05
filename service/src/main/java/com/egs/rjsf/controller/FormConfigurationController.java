package com.egs.rjsf.controller;

import com.egs.rjsf.entity.FormConfiguration;
import com.egs.rjsf.service.FormConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/form-configurations")
@CrossOrigin
public class FormConfigurationController {

    private final FormConfigurationService formConfigurationService;

    public FormConfigurationController(FormConfigurationService formConfigurationService) {
        this.formConfigurationService = formConfigurationService;
    }

    @GetMapping
    public ResponseEntity<List<FormConfiguration>> getAll() {
        return ResponseEntity.ok(formConfigurationService.findAll());
    }

    @GetMapping("/{formKey}")
    public ResponseEntity<FormConfiguration> getByFormKey(@PathVariable String formKey) {
        return ResponseEntity.ok(formConfigurationService.findByFormKey(formKey));
    }

    @PostMapping
    public ResponseEntity<FormConfiguration> create(@RequestBody FormConfiguration formConfiguration) {
        FormConfiguration created = formConfigurationService.create(formConfiguration);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormConfiguration> update(@PathVariable UUID id,
                                                    @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(formConfigurationService.update(id, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        formConfigurationService.softDelete(id);
        return ResponseEntity.ok(Map.of("message", "Form configuration deleted"));
    }
}
