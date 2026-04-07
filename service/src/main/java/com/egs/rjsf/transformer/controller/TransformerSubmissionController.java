package com.egs.rjsf.transformer.controller;

import com.egs.rjsf.transformer.dto.TransformerSubmissionRequest;
import com.egs.rjsf.transformer.dto.TransformerSubmissionResponse;
import com.egs.rjsf.transformer.service.SubmissionReadService;
import com.egs.rjsf.transformer.service.SubmissionWriteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions")
@CrossOrigin
public class TransformerSubmissionController {

    private final SubmissionWriteService writeService;
    private final SubmissionReadService readService;

    public TransformerSubmissionController(SubmissionWriteService writeService,
                                           SubmissionReadService readService) {
        this.writeService = writeService;
        this.readService = readService;
    }

    @PostMapping
    public ResponseEntity<TransformerSubmissionResponse> create(
            @RequestBody TransformerSubmissionRequest request) {
        TransformerSubmissionResponse response = writeService.write(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransformerSubmissionResponse> get(
            @PathVariable long id,
            @RequestParam String formId,
            @RequestParam(required = false) Integer templateVersion) {
        TransformerSubmissionResponse response = readService.read(id, formId, templateVersion);
        return ResponseEntity.ok(response);
    }
}
