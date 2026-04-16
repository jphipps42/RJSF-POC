package com.egs.rjsf.service.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages which relational sync strategy (MAPPER or POJO) is active.
 * Thread-safe — the active mode can be switched at runtime via REST endpoint.
 */
@Service
public class RelationalSyncStrategyManager {

    private static final Logger log = LoggerFactory.getLogger(RelationalSyncStrategyManager.class);

    private final Map<String, RelationalSyncStrategy> strategies;
    private final AtomicReference<String> activeMode = new AtomicReference<>("MAPPER");

    public RelationalSyncStrategyManager(
            @Qualifier("mapperSyncStrategy") RelationalSyncStrategy mapper,
            @Qualifier("pojoSyncStrategy") RelationalSyncStrategy pojo) {
        this.strategies = Map.of("MAPPER", mapper, "POJO", pojo);
    }

    public RelationalSyncStrategy getActiveStrategy() {
        return strategies.get(activeMode.get());
    }

    public String getActiveMode() {
        return activeMode.get();
    }

    public void setActiveMode(String mode) {
        String normalized = mode.toUpperCase();
        if (!strategies.containsKey(normalized)) {
            throw new IllegalArgumentException("Unknown sync mode: " + mode
                    + ". Valid modes: " + strategies.keySet());
        }
        String previous = activeMode.getAndSet(normalized);
        if (!previous.equals(normalized)) {
            log.info("Relational sync mode changed: {} → {}", previous, normalized);
        }
    }
}
