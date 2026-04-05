package com.egs.rjsf.repository;

import com.egs.rjsf.entity.FormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormSubmissionRepository extends JpaRepository<FormSubmission, UUID> {

    List<FormSubmission> findByAwardIdOrderByFormKey(UUID awardId);

    Optional<FormSubmission> findByAwardIdAndFormKey(UUID awardId, String formKey);
}
