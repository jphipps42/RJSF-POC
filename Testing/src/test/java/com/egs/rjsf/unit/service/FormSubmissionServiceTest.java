package com.egs.rjsf.unit.service;

import com.egs.rjsf.entity.FormSchemaVersion;
import com.egs.rjsf.entity.FormSubmission;
import com.egs.rjsf.repository.FormConfigurationRepository;
import com.egs.rjsf.repository.FormSchemaVersionRepository;
import com.egs.rjsf.repository.FormSubmissionRepository;
import com.egs.rjsf.service.FormSubmissionService;
import com.egs.rjsf.transformer.service.SubmissionWriteService;
import com.egs.rjsf.util.MigrationEngine;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FormSubmissionService Unit Tests")
class FormSubmissionServiceTest {

    @Mock private FormSubmissionRepository submissionRepo;
    @Mock private FormSchemaVersionRepository schemaVersionRepo;
    @Mock private FormConfigurationRepository formConfigRepo;
    @Mock private MigrationEngine migrationEngine;
    @Mock private SubmissionWriteService transformerWriteService;

    @InjectMocks private FormSubmissionService service;

    private UUID submissionId;
    private UUID awardId;
    private UUID formConfigId;
    private UUID schemaVersionId;
    private FormSubmission submission;
    private FormSchemaVersion schemaVersion;

    @BeforeEach
    void setUp() {
        submissionId = UUID.randomUUID();
        awardId = UUID.randomUUID();
        formConfigId = UUID.randomUUID();
        schemaVersionId = UUID.randomUUID();

        submission = FormSubmission.builder()
                .id(submissionId)
                .awardId(awardId)
                .formConfigId(formConfigId)
                .formKey("pre_award_composite")
                .formData(new HashMap<>(Map.of("pi_budget", 100000)))
                .status("not_started")
                .sectionStatus(new HashMap<>(Map.of(
                        "overview", "not_started",
                        "safety_review", "not_started"
                )))
                .isLocked(false)
                .build();

        schemaVersion = new FormSchemaVersion();
        schemaVersion.setId(schemaVersionId);
        schemaVersion.setVersion(1);
        schemaVersion.setJsonSchema(Map.of());
        schemaVersion.setUiSchema(Map.of());
    }

    @Nested
    @DisplayName("saveDraft()")
    class SaveDraft {

        @Test
        @DisplayName("saves draft and updates section status to in_progress")
        void savesDraftSuccessfully() {
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.of(submission));
            when(schemaVersionRepo.findByFormIdAndIsCurrentTrue(formConfigId))
                    .thenReturn(Optional.of(schemaVersion));
            when(submissionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> formData = Map.of("pi_budget", 200000, "safety_q1", "yes");
            FormSubmission result = service.saveDraft(submissionId, formData, "overview");

            assertThat(result.getStatus()).isEqualTo("in_progress");
            assertThat(result.getSectionStatus().get("overview")).isEqualTo("in_progress");
            assertThat(result.getFormData()).isEqualTo(formData);
            assertThat(result.getSchemaVersionId()).isEqualTo(schemaVersionId);
        }

        @Test
        @DisplayName("throws EntityNotFoundException for missing submission")
        void throwsWhenNotFound() {
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.saveDraft(submissionId, Map.of(), "overview"))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("throws IllegalStateException when section is locked")
        void throwsWhenSectionLocked() {
            submission.getSectionStatus().put("overview", "submitted");
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.of(submission));

            assertThatThrownBy(() -> service.saveDraft(submissionId, Map.of(), "overview"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("submitted and cannot be edited");
        }

        @Test
        @DisplayName("throws IllegalStateException when entire submission is locked")
        void throwsWhenSubmissionLocked() {
            submission.setIsLocked(true);
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.of(submission));

            assertThatThrownBy(() -> service.saveDraft(submissionId, Map.of(), null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("locked and cannot be edited");
        }

        @Test
        @DisplayName("calls transformer sync after save")
        void callsTransformerSync() {
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.of(submission));
            when(schemaVersionRepo.findByFormIdAndIsCurrentTrue(formConfigId))
                    .thenReturn(Optional.of(schemaVersion));
            when(submissionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.saveDraft(submissionId, Map.of("pi_budget", 500), "overview");

            verify(transformerWriteService).writeSection(
                    eq("pre-award-overview"),
                    eq(awardId),
                    any(),
                    eq("overview"),
                    isNull()
            );
        }

        @Test
        @DisplayName("transformer failure does not prevent JSONB save")
        void transformerFailureDoesNotBreakSave() {
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.of(submission));
            when(schemaVersionRepo.findByFormIdAndIsCurrentTrue(formConfigId))
                    .thenReturn(Optional.of(schemaVersion));
            when(submissionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doThrow(new RuntimeException("DB connection failed"))
                    .when(transformerWriteService).writeSection(any(), any(), any(), any(), any());

            FormSubmission result = service.saveDraft(submissionId, Map.of("pi_budget", 500), "overview");

            assertThat(result.getStatus()).isEqualTo("in_progress");
            verify(submissionRepo).save(any());
        }
    }

    @Nested
    @DisplayName("submit()")
    class Submit {

        @Test
        @DisplayName("submits a single section and marks it as submitted")
        void submitsSingleSection() {
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.of(submission));
            when(schemaVersionRepo.findByFormIdAndIsCurrentTrue(formConfigId))
                    .thenReturn(Optional.of(schemaVersion));
            when(submissionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FormSubmission result = service.submit(submissionId, Map.of(), "overview");

            assertThat(result.getSectionStatus().get("overview")).isEqualTo("submitted");
            assertThat(result.getStatus()).isEqualTo("in_progress");
            assertThat(result.getIsLocked()).isFalse();
        }

        @Test
        @DisplayName("locks submission when all sections are submitted")
        void locksWhenAllSubmitted() {
            submission.setSectionStatus(new HashMap<>(Map.of(
                    "overview", "submitted",
                    "safety_review", "not_started"
            )));
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.of(submission));
            when(schemaVersionRepo.findByFormIdAndIsCurrentTrue(formConfigId))
                    .thenReturn(Optional.of(schemaVersion));
            when(submissionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FormSubmission result = service.submit(submissionId, Map.of(), "safety_review");

            assertThat(result.getStatus()).isEqualTo("submitted");
            assertThat(result.getIsLocked()).isTrue();
            assertThat(result.getSubmittedAt()).isNotNull();
            assertThat(result.getCompletionDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("reset()")
    class Reset {

        @Test
        @DisplayName("resets a single section to in_progress for editing")
        void resetsSingleSection() {
            submission.getSectionStatus().put("overview", "submitted");
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.of(submission));
            when(submissionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FormSubmission result = service.reset(submissionId, "overview");

            assertThat(result.getSectionStatus().get("overview")).isEqualTo("in_progress");
            assertThat(result.getIsLocked()).isFalse();
        }

        @Test
        @DisplayName("resets entire submission — unlocks all submitted sections")
        void resetsEntireSubmission() {
            submission.setStatus("submitted");
            submission.setIsLocked(true);
            submission.getSectionStatus().put("overview", "submitted");
            submission.getSectionStatus().put("safety_review", "submitted");
            when(submissionRepo.findById(submissionId)).thenReturn(Optional.of(submission));
            when(submissionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FormSubmission result = service.reset(submissionId, null);

            assertThat(result.getStatus()).isEqualTo("in_progress");
            assertThat(result.getIsLocked()).isFalse();
            assertThat(result.getSubmittedAt()).isNull();
            assertThat(result.getSectionStatus().get("overview")).isEqualTo("in_progress");
            assertThat(result.getSectionStatus().get("safety_review")).isEqualTo("in_progress");
        }
    }
}
