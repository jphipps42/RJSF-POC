package com.egs.rjsf.entity.formdata;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "form_pre_award_animal")
@Getter @Setter @NoArgsConstructor
public class FormPreAwardAnimal extends FormDataBase {

    @Column(name = "animal_q1")
    private String animalQ1;

    @Type(JsonType.class)
    @Column(name = "animal_species", columnDefinition = "jsonb")
    private Object animalSpecies;

    @Column(name = "animal_q2")
    private String animalQ2;

    @Column(name = "animal_q3")
    private String animalQ3;

    @Column(name = "animal_q4")
    private String animalQ4;

    @Column(name = "iacuc_protocol_number")
    private String iacucProtocolNumber;

    @Column(name = "animal_q5")
    private String animalQ5;

    @Column(name = "animal_start_date")
    private String animalStartDate;

    @Column(name = "animal_notes")
    private String animalNotes;
}
