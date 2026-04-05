package com.egs.rjsf.repository;

import com.egs.rjsf.entity.ProjectPersonnel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectPersonnelRepository extends JpaRepository<ProjectPersonnel, UUID> {

    List<ProjectPersonnel> findByAwardIdOrderByName(UUID awardId);

    List<ProjectPersonnel> findAllByOrderByName();
}
