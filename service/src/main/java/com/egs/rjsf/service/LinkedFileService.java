package com.egs.rjsf.service;

import com.egs.rjsf.entity.AwardLinkedFile;
import com.egs.rjsf.repository.AwardLinkedFileRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LinkedFileService {

    private final AwardLinkedFileRepository awardLinkedFileRepository;

    public LinkedFileService(AwardLinkedFileRepository awardLinkedFileRepository) {
        this.awardLinkedFileRepository = awardLinkedFileRepository;
    }

    public List<AwardLinkedFile> findByAwardId(UUID awardId) {
        return awardLinkedFileRepository.findByAwardIdOrderBySectionAscCreatedAtDesc(awardId);
    }

    public List<AwardLinkedFile> findByAwardIdAndSection(UUID awardId, String section) {
        return awardLinkedFileRepository.findByAwardIdAndSectionOrderByCreatedAtDesc(awardId, section);
    }

    public AwardLinkedFile create(AwardLinkedFile file) {
        return awardLinkedFileRepository.save(file);
    }

    public AwardLinkedFile update(UUID id, Map<String, Object> updates) {
        AwardLinkedFile file = awardLinkedFileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "AwardLinkedFile not found: " + id));

        if (updates.containsKey("file_name")) {
            file.setFileName((String) updates.get("file_name"));
        }
        if (updates.containsKey("description")) {
            file.setDescription((String) updates.get("description"));
        }

        file.setLastUpdated(OffsetDateTime.now());
        return awardLinkedFileRepository.save(file);
    }

    public void delete(UUID id) {
        AwardLinkedFile file = awardLinkedFileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "AwardLinkedFile not found: " + id));
        awardLinkedFileRepository.delete(file);
    }
}
