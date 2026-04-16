package com.egs.rjsf.service.sync;

import com.egs.rjsf.transformer.service.SubmissionWriteService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Wraps the existing transformer pipeline (Map-based field mapping + raw JDBC).
 */
@Component("mapperSyncStrategy")
public class MapperSyncStrategy implements RelationalSyncStrategy {

    private final SubmissionWriteService submissionWriteService;

    public MapperSyncStrategy(SubmissionWriteService submissionWriteService) {
        this.submissionWriteService = submissionWriteService;
    }

    @Override
    public String getName() {
        return "MAPPER";
    }

    @Override
    public void writeSection(String formId, UUID awardId, Map<String, Object> formData,
                             String sectionId, String submittedBy) {
        submissionWriteService.writeSection(formId, awardId, formData, sectionId, submittedBy);
    }
}
