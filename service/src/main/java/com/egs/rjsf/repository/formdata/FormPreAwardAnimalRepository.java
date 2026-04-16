package com.egs.rjsf.repository.formdata;

import com.egs.rjsf.entity.formdata.FormPreAwardAnimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormPreAwardAnimalRepository extends JpaRepository<FormPreAwardAnimal, Long> {
    Optional<FormPreAwardAnimal> findByAwardId(UUID awardId);
}
