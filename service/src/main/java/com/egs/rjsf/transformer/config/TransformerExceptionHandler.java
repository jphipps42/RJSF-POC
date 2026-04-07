package com.egs.rjsf.transformer.config;

import com.egs.rjsf.transformer.exception.DuplicateSubmissionException;
import com.egs.rjsf.transformer.exception.SubmissionNotFoundException;
import com.egs.rjsf.transformer.exception.TemplateNotFoundException;
import com.egs.rjsf.transformer.exception.TransformerValidationException;
import com.egs.rjsf.transformer.exception.VersionMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.egs.rjsf.transformer")
public class TransformerExceptionHandler {

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTemplateNotFound(TemplateNotFoundException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "TEMPLATE_NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(TransformerValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(TransformerValidationException e) {
        List<Map<String, String>> violations = e.getViolations().stream()
                .map(v -> Map.of("json_path", v.jsonPath(), "message", v.message()))
                .toList();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "VALIDATION_FAILED");
        body.put("violations", violations);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(VersionMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleVersionMismatch(VersionMismatchException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "VERSION_MISMATCH", "message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateSubmissionException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateSubmissionException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "DUPLICATE_SUBMISSION", "message", e.getMessage()));
    }

    @ExceptionHandler(SubmissionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(SubmissionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "SUBMISSION_NOT_FOUND", "message", e.getMessage()));
    }
}
