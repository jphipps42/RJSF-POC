package com.egs.rjsf.service;

import com.egs.rjsf.entity.ProjectPersonnel;
import com.egs.rjsf.repository.ProjectPersonnelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PersonnelService {

    private final ProjectPersonnelRepository projectPersonnelRepository;

    public PersonnelService(ProjectPersonnelRepository projectPersonnelRepository) {
        this.projectPersonnelRepository = projectPersonnelRepository;
    }

    public List<ProjectPersonnel> findByAwardId(UUID awardId) {
        return projectPersonnelRepository.findByAwardIdOrderByName(awardId);
    }

    public List<ProjectPersonnel> findAll() {
        return projectPersonnelRepository.findAllByOrderByName();
    }

    public ProjectPersonnel create(ProjectPersonnel personnel) {
        return projectPersonnelRepository.save(personnel);
    }

    public ProjectPersonnel update(UUID id, Map<String, Object> updates) {
        ProjectPersonnel personnel = projectPersonnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ProjectPersonnel not found: " + id));

        if (updates.containsKey("organization")) {
            personnel.setOrganization((String) updates.get("organization"));
        }
        if (updates.containsKey("country")) {
            personnel.setCountry((String) updates.get("country"));
        }
        if (updates.containsKey("project_role")) {
            personnel.setProjectRole((String) updates.get("project_role"));
        }
        if (updates.containsKey("name")) {
            personnel.setName((String) updates.get("name"));
        }
        if (updates.containsKey("is_subcontract")) {
            personnel.setIsSubcontract((Boolean) updates.get("is_subcontract"));
        }

        return projectPersonnelRepository.save(personnel);
    }

    public void delete(UUID id) {
        ProjectPersonnel personnel = projectPersonnelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "ProjectPersonnel not found: " + id));
        projectPersonnelRepository.delete(personnel);
    }
}
