package com.egs.rjsf.controller;

import com.egs.rjsf.service.export.FormExportService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/export")
@CrossOrigin
public class ExportController {

    private final FormExportService exportService;

    public ExportController(FormExportService exportService) {
        this.exportService = exportService;
    }

    /**
     * Generate PDF and HTML exports for an award.
     * Called when "Submit Recommendation to DHACA R&D" is clicked.
     */
    @PostMapping("/generate/{awardId}")
    public Map<String, String> generateExports(@PathVariable UUID awardId) {
        String filename = exportService.generateExports(awardId);
        return Map.of("filename", filename, "status", "generated");
    }

    /**
     * Retrieve a previously generated PDF by award number.
     */
    @GetMapping("/pdf/{awardNumber}")
    public ResponseEntity<Resource> getPdf(@PathVariable String awardNumber) {
        Path path = exportService.getPdfPath(awardNumber);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + awardNumber + ".pdf\"")
                .body(new FileSystemResource(path));
    }

    /**
     * Retrieve a previously generated HTML by award number.
     */
    @GetMapping("/html/{awardNumber}")
    public ResponseEntity<Resource> getHtml(@PathVariable String awardNumber) {
        Path path = exportService.getHtmlPath(awardNumber);
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + awardNumber + ".html\"")
                .body(new FileSystemResource(path));
    }
}
