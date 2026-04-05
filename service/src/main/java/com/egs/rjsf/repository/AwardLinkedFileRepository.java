package com.egs.rjsf.repository;

import com.egs.rjsf.entity.AwardLinkedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AwardLinkedFileRepository extends JpaRepository<AwardLinkedFile, UUID> {

    List<AwardLinkedFile> findByAwardIdOrderBySectionAscCreatedAtDesc(UUID awardId);

    List<AwardLinkedFile> findByAwardIdAndSectionOrderByCreatedAtDesc(UUID awardId, String section);
}
