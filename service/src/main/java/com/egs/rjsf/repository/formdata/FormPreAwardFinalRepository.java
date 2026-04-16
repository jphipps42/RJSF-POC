package com.egs.rjsf.repository.formdata;

import com.egs.rjsf.entity.formdata.FormPreAwardFinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormPreAwardFinalRepository extends JpaRepository<FormPreAwardFinal, Long> {
    Optional<FormPreAwardFinal> findByAwardId(UUID awardId);
}
