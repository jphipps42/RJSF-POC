package com.egs.rjsf.entity.formdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "form_pre_award_safety")
@Getter @Setter @NoArgsConstructor
public class FormPreAwardSafety extends FormDataBase {

    @Column(name = "safety_q1")
    private String safetyQ1;

    @Column(name = "programmatic_rec")
    private String programmaticRec;

    @Column(name = "safety_q2")
    private String safetyQ2;

    @Column(name = "safety_q3")
    private String safetyQ3;

    @Column(name = "safety_q4")
    private String safetyQ4;

    @Column(name = "safety_q5")
    private String safetyQ5;

    @Column(name = "safety_q6")
    private String safetyQ6;

    @Column(name = "safety_q7")
    private String safetyQ7;

    @Column(name = "safety_q8")
    private String safetyQ8;

    @Column(name = "safety_notes")
    private String safetyNotes;
}
