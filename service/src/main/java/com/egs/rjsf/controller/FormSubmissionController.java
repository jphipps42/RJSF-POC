package com.egs.rjsf.controller;

import com.egs.rjsf.dto.MigrationResultDto;
import com.egs.rjsf.dto.SubmissionWithSchemaDto;
import com.egs.rjsf.entity.FormSubmission;
import com.egs.rjsf.service.FormSubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/form-submissions")
@CrossOrigin
public class FormSubmissionController {

    private final FormSubmissionService formSubmissionService;

    public FormSubmissionController(FormSubmissionService formSubmissionService) {
        this.formSubmissionService = formSubmissionService;
    }

    public record SaveFormRequest(Map<String, Object> formData) {}

    @GetMapping
    public ResponseEntity<List<SubmissionWithSchemaDto>> getAll(
            @RequestParam(name = "award_id", required = false) UUID awardId) {
        return ResponseEntity.ok(formSubmissionService.findByAwardId(awardId));
    }

    @GetMapping("/by-award/{awardId}/{formKey}")
    public ResponseEntity<SubmissionWithSchemaDto> getByAwardAndFormKey(
            @PathVariable UUID awardId,
            @PathVariable String formKey) {
        return ResponseEntity.ok(formSubmissionService.findByAwardIdAndFormKey(awardId, formKey));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionWithSchemaDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(formSubmissionService.findById(id));
    }

    @GetMapping("/{id}/for-edit")
    public ResponseEntity<MigrationResultDto> getForEdit(@PathVariable UUID id) {
        return ResponseEntity.ok(formSubmissionService.getForEdit(id));
    }

    @GetMapping("/{id}/audit")
    public ResponseEntity<MigrationResultDto> getAudit(@PathVariable UUID id) {
        return ResponseEntity.ok(formSubmissionService.getForAudit(id));
    }

    @PutMapping("/{id}/save")
    public ResponseEntity<FormSubmission> save(
            @PathVariable UUID id,
            @RequestParam(name = "section", required = false) String section,
            @RequestBody SaveFormRequest request) {
        FormSubmission saved = formSubmissionService.saveDraft(id, request.formData(), section);
        try { formSubmissionService.syncToRelationalTable(saved, section); } catch (Throwable ignored) {}
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/submit")
    public ResponseEntity<FormSubmission> submit(
            @PathVariable UUID id,
            @RequestParam(name = "section", required = false) String section,
            @RequestBody SaveFormRequest request) {
        FormSubmission saved = formSubmissionService.submit(id, request.formData(), section);
        try { formSubmissionService.syncToRelationalTable(saved, section); } catch (Throwable ignored) {}
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/reset")
    public ResponseEntity<FormSubmission> reset(
            @PathVariable UUID id,
            @RequestParam(name = "section", required = false) String section) {
        return ResponseEntity.ok(formSubmissionService.reset(id, section));
    }
}
