package com.egs.rjsf.repository.formdata;

import com.egs.rjsf.entity.formdata.FormPreAwardSafety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormPreAwardSafetyRepository extends JpaRepository<FormPreAwardSafety, Long> {
    Optional<FormPreAwardSafety> findByAwardId(UUID awardId);
}
