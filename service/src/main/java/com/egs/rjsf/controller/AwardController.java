package com.egs.rjsf.controller;

import com.egs.rjsf.dto.AwardDetailDto;
import com.egs.rjsf.entity.Award;
import com.egs.rjsf.service.AwardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/awards")
@CrossOrigin
public class AwardController {

    private final AwardService awardService;

    public AwardController(AwardService awardService) {
        this.awardService = awardService;
    }

    @GetMapping
    public ResponseEntity<List<Award>> getAll() {
        return ResponseEntity.ok(awardService.findAll());
    }

    @GetMapping("/by-log/{logNumber}")
    public ResponseEntity<AwardDetailDto> getByLogNumber(@PathVariable String logNumber) {
        return ResponseEntity.ok(awardService.findByLogNumberWithDetails(logNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AwardDetailDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(awardService.findByIdWithDetails(id));
    }

    @PostMapping
    public ResponseEntity<Award> create(@RequestBody Award award) {
        Award created = awardService.create(award);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Award> update(@PathVariable UUID id,
                                        @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(awardService.update(id, updates));
    }
}
