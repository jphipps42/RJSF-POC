package com.egs.rjsf.repository.formdata;

import com.egs.rjsf.entity.formdata.FormPreAwardOverview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormPreAwardOverviewRepository extends JpaRepository<FormPreAwardOverview, Long> {
    Optional<FormPreAwardOverview> findByAwardId(UUID awardId);
}
