package com.egs.rjsf.repository.formdata;

import com.egs.rjsf.entity.formdata.FormPreAwardHuman;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormPreAwardHumanRepository extends JpaRepository<FormPreAwardHuman, Long> {
    Optional<FormPreAwardHuman> findByAwardId(UUID awardId);
}
