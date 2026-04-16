package com.egs.rjsf.controller;

import com.egs.rjsf.service.sync.RelationalSyncStrategyManager;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sync-mode")
@CrossOrigin
public class SyncModeController {

    private final RelationalSyncStrategyManager manager;

    public SyncModeController(RelationalSyncStrategyManager manager) {
        this.manager = manager;
    }

    @GetMapping
    public Map<String, String> getMode() {
        return Map.of("mode", manager.getActiveMode());
    }

    @PutMapping
    public Map<String, String> setMode(@RequestBody Map<String, String> body) {
        manager.setActiveMode(body.get("mode"));
        return Map.of("mode", manager.getActiveMode());
    }
}
