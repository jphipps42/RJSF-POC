package com.egs.rjsf.entity.formdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "form_pre_award_human")
@Getter @Setter @NoArgsConstructor
public class FormPreAwardHuman extends FormDataBase {

    // C.a No Regulatory Review Required
    @Column(name = "no_review_default_no")
    private Boolean noReviewDefaultNo;

    @Column(name = "human_s1_q1")
    private String humanS1Q1;

    @Column(name = "human_s1_q2")
    private String humanS1Q2;

    @Column(name = "human_s1_q3")
    private String humanS1Q3;

    @Column(name = "human_s1_q4")
    private String humanS1Q4;

    @Column(name = "human_s1_q5")
    private String humanS1Q5;

    @Column(name = "human_s1_notes")
    private String humanS1Notes;

    // C.b Human Anatomical Substances
    @Column(name = "has_default_no")
    private Boolean hasDefaultNo;

    @Column(name = "human_has_q1")
    private String humanHasQ1;

    @Column(name = "human_has_q2")
    private String humanHasQ2;

    @Column(name = "human_has_q3")
    private String humanHasQ3;

    @Column(name = "human_has_q4")
    private String humanHasQ4;

    @Column(name = "human_has_q5")
    private String humanHasQ5;

    @Column(name = "human_has_q6")
    private String humanHasQ6;

    @Column(name = "human_has_q7")
    private String humanHasQ7;

    @Column(name = "human_has_notes")
    private String humanHasNotes;

    // C.c Human Data - Secondary Use
    @Column(name = "human_ds_q1")
    private String humanDsQ1;

    @Column(name = "human_ds_notes")
    private String humanDsNotes;

    // C.d Human Subjects
    @Column(name = "human_hs_q1")
    private String humanHsQ1;

    @Column(name = "human_hs_q2")
    private String humanHsQ2;

    @Column(name = "ct_fda_q1")
    private String ctFdaQ1;

    @Column(name = "ct_nonus_q1")
    private String ctNonusQ1;

    @Column(name = "human_hs_notes")
    private String humanHsNotes;

    // C.e Other/Special Topics
    @Column(name = "human_ost_q1")
    private String humanOstQ1;

    @Column(name = "human_ost_notes")
    private String humanOstNotes;

    // C.f Estimated Start
    @Column(name = "estimated_start_date")
    private String estimatedStartDate;
}
