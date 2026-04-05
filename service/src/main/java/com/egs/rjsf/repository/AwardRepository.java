package com.egs.rjsf.repository;

import com.egs.rjsf.entity.Award;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AwardRepository extends JpaRepository<Award, UUID> {

    Optional<Award> findByLogNumber(String logNumber);

    List<Award> findAllByOrderByCreatedAtDesc();
}
