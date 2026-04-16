package com.egs.rjsf.unit.sync;

import com.egs.rjsf.entity.formdata.*;
import com.egs.rjsf.repository.formdata.*;
import com.egs.rjsf.service.sync.PojoSyncStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PojoSyncStrategy Unit Tests")
class PojoSyncStrategyTest {

    @Mock private FormPreAwardOverviewRepository overviewRepo;
    @Mock private FormPreAwardSafetyRepository safetyRepo;
    @Mock private FormPreAwardAnimalRepository animalRepo;
    @Mock private FormPreAwardHumanRepository humanRepo;
    @Mock private FormPreAwardAcquisitionRepository acquisitionRepo;
    @Mock private FormPreAwardFinalRepository finalRepo;

    private PojoSyncStrategy strategy;
    private UUID awardId;

    @BeforeEach
    void setUp() {
        strategy = new PojoSyncStrategy(
                overviewRepo, safetyRepo, animalRepo,
                humanRepo, acquisitionRepo, finalRepo);
        awardId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getName() returns POJO")
    void nameIsPojo() {
        assertThat(strategy.getName()).isEqualTo("POJO");
    }

    @Nested
    @DisplayName("Overview section")
    class Overview {

        @Test
        @DisplayName("creates new entity when none exists for award")
        void createsNewEntity() {
            when(overviewRepo.findByAwardId(awardId)).thenReturn(Optional.empty());
            when(overviewRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> formData = Map.of(
                    "pi_budget", 500000,
                    "program_manager", "Test PM",
                    "prime_award_type", "extramural"
            );

            strategy.writeSection("pre-award-overview", awardId, formData, "overview", "testuser");

            ArgumentCaptor<FormPreAwardOverview> captor = ArgumentCaptor.forClass(FormPreAwardOverview.class);
            verify(overviewRepo).save(captor.capture());
            FormPreAwardOverview saved = captor.getValue();

            assertThat(saved.getAwardId()).isEqualTo(awardId);
            assertThat(saved.getPiBudget()).isEqualByComparingTo(new BigDecimal("500000"));
            assertThat(saved.getProgramManager()).isEqualTo("Test PM");
            assertThat(saved.getPrimeAwardType()).isEqualTo("extramural");
            assertThat(saved.getSchemaVersion()).isEqualTo(1);
            assertThat(saved.getSubmittedBy()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("updates existing entity")
        void updatesExistingEntity() {
            FormPreAwardOverview existing = new FormPreAwardOverview();
            existing.setAwardId(awardId);
            existing.setProgramManager("Old PM");
            when(overviewRepo.findByAwardId(awardId)).thenReturn(Optional.of(existing));
            when(overviewRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            strategy.writeSection("pre-award-overview", awardId,
                    Map.of("program_manager", "New PM"), "overview", null);

            verify(overviewRepo).save(existing);
            assertThat(existing.getProgramManager()).isEqualTo("New PM");
        }
    }

    @Nested
    @DisplayName("Safety section")
    class Safety {

        @Test
        @DisplayName("maps all safety fields")
        void mapsAllFields() {
            when(safetyRepo.findByAwardId(awardId)).thenReturn(Optional.empty());
            when(safetyRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> formData = new HashMap<>();
            formData.put("safety_q1", "yes");
            formData.put("safety_q2", "no");
            formData.put("safety_notes", "Some note");

            strategy.writeSection("pre-award-safety", awardId, formData, "safety_review", null);

            ArgumentCaptor<FormPreAwardSafety> captor = ArgumentCaptor.forClass(FormPreAwardSafety.class);
            verify(safetyRepo).save(captor.capture());
            FormPreAwardSafety saved = captor.getValue();

            assertThat(saved.getSafetyQ1()).isEqualTo("yes");
            assertThat(saved.getSafetyQ2()).isEqualTo("no");
            assertThat(saved.getSafetyNotes()).isEqualTo("Some note");
        }
    }

    @Nested
    @DisplayName("Human section (multi-section table)")
    class Human {

        @Test
        @DisplayName("only sets no_regulatory fields when sectionId is human_no_regulatory")
        void setsOnlyNoRegulatoryFields() {
            FormPreAwardHuman existing = new FormPreAwardHuman();
            existing.setAwardId(awardId);
            existing.setHumanHsQ1("yes"); // pre-existing data from another section
            when(humanRepo.findByAwardId(awardId)).thenReturn(Optional.of(existing));
            when(humanRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> formData = new HashMap<>();
            formData.put("no_review_default_no", true);
            formData.put("human_s1_q1", "no");

            strategy.writeSection("pre-award-human", awardId, formData,
                    "human_no_regulatory", null);

            verify(humanRepo).save(existing);
            assertThat(existing.getNoReviewDefaultNo()).isTrue();
            assertThat(existing.getHumanS1Q1()).isEqualTo("no");
            // Other section's data should be untouched
            assertThat(existing.getHumanHsQ1()).isEqualTo("yes");
        }

        @Test
        @DisplayName("only sets anatomical fields when sectionId is human_anatomical")
        void setsOnlyAnatomicalFields() {
            FormPreAwardHuman existing = new FormPreAwardHuman();
            existing.setAwardId(awardId);
            existing.setHumanS1Q1("yes"); // pre-existing from no_regulatory
            when(humanRepo.findByAwardId(awardId)).thenReturn(Optional.of(existing));
            when(humanRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> formData = new HashMap<>();
            formData.put("has_default_no", false);
            formData.put("human_has_q1", "yes");

            strategy.writeSection("pre-award-human", awardId, formData,
                    "human_anatomical", null);

            verify(humanRepo).save(existing);
            assertThat(existing.getHasDefaultNo()).isFalse();
            assertThat(existing.getHumanHasQ1()).isEqualTo("yes");
            // no_regulatory data untouched
            assertThat(existing.getHumanS1Q1()).isEqualTo("yes");
        }
    }

    @Nested
    @DisplayName("Acquisition section (multi-section table)")
    class Acquisition {

        @Test
        @DisplayName("only sets personnel fields when sectionId is acq_br_personnel")
        void setsOnlyPersonnelFields() {
            when(acquisitionRepo.findByAwardId(awardId)).thenReturn(Optional.empty());
            when(acquisitionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> formData = Map.of(
                    "acq_personnel_qualifications", "yes",
                    "acq_personnel_notes", "Looks good"
            );

            strategy.writeSection("pre-award-acquisition", awardId, formData,
                    "acq_br_personnel", null);

            ArgumentCaptor<FormPreAwardAcquisition> captor =
                    ArgumentCaptor.forClass(FormPreAwardAcquisition.class);
            verify(acquisitionRepo).save(captor.capture());
            FormPreAwardAcquisition saved = captor.getValue();

            assertThat(saved.getAcqPersonnelQualifications()).isEqualTo("yes");
            assertThat(saved.getAcqPersonnelNotes()).isEqualTo("Looks good");
            // Other sections should be null (new entity)
            assertThat(saved.getAcqEquipIncluded()).isNull();
            assertThat(saved.getAcqPeerReviewScore()).isNull();
        }

        @Test
        @DisplayName("handles peer review score as BigDecimal")
        void handlesPeerReviewScore() {
            when(acquisitionRepo.findByAwardId(awardId)).thenReturn(Optional.empty());
            when(acquisitionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> formData = Map.of(
                    "acq_peer_review_score", 8.5,
                    "acq_peer_review_outcome", "fund"
            );

            strategy.writeSection("pre-award-acquisition", awardId, formData,
                    "acq_peer_review", null);

            ArgumentCaptor<FormPreAwardAcquisition> captor =
                    ArgumentCaptor.forClass(FormPreAwardAcquisition.class);
            verify(acquisitionRepo).save(captor.capture());

            assertThat(captor.getValue().getAcqPeerReviewScore())
                    .isEqualByComparingTo(new BigDecimal("8.5"));
            assertThat(captor.getValue().getAcqPeerReviewOutcome()).isEqualTo("fund");
        }
    }

    @Nested
    @DisplayName("Final Recommendation section")
    class FinalRec {

        @Test
        @DisplayName("maps all final recommendation fields")
        void mapsAllFields() {
            when(finalRepo.findByAwardId(awardId)).thenReturn(Optional.empty());
            when(finalRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> formData = Map.of(
                    "scientific_overlap", "no",
                    "foreign_involvement", "yes",
                    "so_recommendation", "approval",
                    "gor_comments", "Approved by GOR"
            );

            strategy.writeSection("pre-award-final", awardId, formData,
                    "final_recommendation", null);

            ArgumentCaptor<FormPreAwardFinal> captor = ArgumentCaptor.forClass(FormPreAwardFinal.class);
            verify(finalRepo).save(captor.capture());
            FormPreAwardFinal saved = captor.getValue();

            assertThat(saved.getScientificOverlap()).isEqualTo("no");
            assertThat(saved.getForeignInvolvement()).isEqualTo("yes");
            assertThat(saved.getSoRecommendation()).isEqualTo("approval");
            assertThat(saved.getGorComments()).isEqualTo("Approved by GOR");
        }
    }

    @Test
    @DisplayName("unknown formId logs warning and does not throw")
    void unknownFormIdDoesNotThrow() {
        assertThatCode(() -> strategy.writeSection(
                "unknown-form", awardId, Map.of(), "whatever", null))
                .doesNotThrowAnyException();
    }
}
