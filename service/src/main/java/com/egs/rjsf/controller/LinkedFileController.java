package com.egs.rjsf.controller;

import com.egs.rjsf.entity.AwardLinkedFile;
import com.egs.rjsf.service.LinkedFileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/linked-files")
@CrossOrigin
public class LinkedFileController {

    private final LinkedFileService linkedFileService;

    public LinkedFileController(LinkedFileService linkedFileService) {
        this.linkedFileService = linkedFileService;
    }

    @GetMapping
    public ResponseEntity<List<AwardLinkedFile>> getAll(
            @RequestParam(name = "award_id", required = false) UUID awardId,
            @RequestParam(name = "section", required = false) String section) {
        if (awardId != null && section != null) {
            return ResponseEntity.ok(linkedFileService.findByAwardIdAndSection(awardId, section));
        } else if (awardId != null) {
            return ResponseEntity.ok(linkedFileService.findByAwardId(awardId));
        }
        return ResponseEntity.ok(linkedFileService.findByAwardId(awardId));
    }

    @PostMapping
    public ResponseEntity<AwardLinkedFile> create(@RequestBody AwardLinkedFile linkedFile) {
        AwardLinkedFile created = linkedFileService.create(linkedFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AwardLinkedFile> update(@PathVariable UUID id,
                                                  @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(linkedFileService.update(id, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        linkedFileService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Linked file deleted"));
    }
}
