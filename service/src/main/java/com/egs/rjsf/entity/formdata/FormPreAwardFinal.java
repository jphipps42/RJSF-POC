package com.egs.rjsf.entity.formdata;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "form_pre_award_final")
@Getter @Setter @NoArgsConstructor
public class FormPreAwardFinal extends FormDataBase {

    @Column(name = "scientific_overlap")
    private String scientificOverlap;

    @Column(name = "foreign_involvement")
    private String foreignInvolvement;

    @Column(name = "risg_approval")
    private String risgApproval;

    @Column(name = "so_recommendation")
    private String soRecommendation;

    @Column(name = "so_comments")
    private String soComments;

    @Column(name = "gor_recommendation")
    private String gorRecommendation;

    @Column(name = "gor_comments")
    private String gorComments;
}
