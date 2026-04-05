package com.egs.rjsf.controller;

import com.egs.rjsf.entity.ProjectPersonnel;
import com.egs.rjsf.service.PersonnelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/personnel")
@CrossOrigin
public class PersonnelController {

    private final PersonnelService personnelService;

    public PersonnelController(PersonnelService personnelService) {
        this.personnelService = personnelService;
    }

    @GetMapping
    public ResponseEntity<List<ProjectPersonnel>> getAll(
            @RequestParam(name = "award_id", required = false) UUID awardId) {
        if (awardId != null) {
            return ResponseEntity.ok(personnelService.findByAwardId(awardId));
        }
        return ResponseEntity.ok(personnelService.findAll());
    }

    @PostMapping
    public ResponseEntity<ProjectPersonnel> create(@RequestBody ProjectPersonnel personnel) {
        ProjectPersonnel created = personnelService.create(personnel);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectPersonnel> update(@PathVariable UUID id,
                                                   @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(personnelService.update(id, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable UUID id) {
        personnelService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Personnel deleted"));
    }
}
