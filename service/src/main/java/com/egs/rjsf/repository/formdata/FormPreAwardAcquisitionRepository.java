package com.egs.rjsf.repository.formdata;

import com.egs.rjsf.entity.formdata.FormPreAwardAcquisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormPreAwardAcquisitionRepository extends JpaRepository<FormPreAwardAcquisition, Long> {
    Optional<FormPreAwardAcquisition> findByAwardId(UUID awardId);
}
