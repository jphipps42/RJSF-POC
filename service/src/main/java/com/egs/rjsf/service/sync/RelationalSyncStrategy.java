package com.egs.rjsf.service.sync;

import java.util.Map;
import java.util.UUID;

/**
 * Strategy interface for syncing JSONB form data to relational tables.
 * Two implementations: MapperSyncStrategy (existing transformer pipeline)
 * and PojoSyncStrategy (JPA entity-based).
 */
public interface RelationalSyncStrategy {

    String getName();

    void writeSection(String formId, UUID awardId, Map<String, Object> formData,
                      String sectionId, String submittedBy);
}
