package com.egs.rjsf.unit.sync;

import com.egs.rjsf.service.sync.RelationalSyncStrategy;
import com.egs.rjsf.service.sync.RelationalSyncStrategyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RelationalSyncStrategyManager Unit Tests")
class RelationalSyncStrategyManagerTest {

    @Mock private RelationalSyncStrategy mapperStrategy;
    @Mock private RelationalSyncStrategy pojoStrategy;

    private RelationalSyncStrategyManager manager;

    @BeforeEach
    void setUp() {
        manager = new RelationalSyncStrategyManager(mapperStrategy, pojoStrategy);
    }

    @Test
    @DisplayName("defaults to MAPPER mode")
    void defaultsToMapper() {
        assertThat(manager.getActiveMode()).isEqualTo("MAPPER");
        assertThat(manager.getActiveStrategy()).isSameAs(mapperStrategy);
    }

    @Test
    @DisplayName("switches to POJO mode")
    void switchesToPojo() {
        manager.setActiveMode("POJO");

        assertThat(manager.getActiveMode()).isEqualTo("POJO");
        assertThat(manager.getActiveStrategy()).isSameAs(pojoStrategy);
    }

    @Test
    @DisplayName("switches back to MAPPER from POJO")
    void switchesBackToMapper() {
        manager.setActiveMode("POJO");
        manager.setActiveMode("MAPPER");

        assertThat(manager.getActiveMode()).isEqualTo("MAPPER");
        assertThat(manager.getActiveStrategy()).isSameAs(mapperStrategy);
    }

    @Test
    @DisplayName("accepts lowercase mode name")
    void acceptsLowercase() {
        manager.setActiveMode("pojo");
        assertThat(manager.getActiveMode()).isEqualTo("POJO");
    }

    @Test
    @DisplayName("throws for unknown mode")
    void throwsForUnknownMode() {
        assertThatThrownBy(() -> manager.setActiveMode("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown sync mode");
    }

    @Test
    @DisplayName("setting same mode is a no-op")
    void sameModIsNoOp() {
        manager.setActiveMode("MAPPER");
        assertThat(manager.getActiveMode()).isEqualTo("MAPPER");
    }
}
