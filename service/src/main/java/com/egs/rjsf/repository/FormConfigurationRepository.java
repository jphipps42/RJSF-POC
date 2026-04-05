package com.egs.rjsf.repository;

import com.egs.rjsf.entity.FormConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FormConfigurationRepository extends JpaRepository<FormConfiguration, UUID> {

    Optional<FormConfiguration> findByFormKeyAndIsActiveTrue(String formKey);

    List<FormConfiguration> findByIsActiveTrueOrderByFormKey();
}
