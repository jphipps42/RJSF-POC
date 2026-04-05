package com.egs.rjsf.dto;

import com.egs.rjsf.entity.AwardLinkedFile;
import com.egs.rjsf.entity.ProjectPersonnel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AwardDetailDto {

    private UUID id;
    private String logNumber;
    private String awardNumber;
    private String awardMechanism;
    private String fundingOpportunity;
    private String principalInvestigator;
    private String performingOrganization;
    private String contractingOrganization;
    private String periodOfPerformance;
    private BigDecimal awardAmount;
    private String programOffice;
    private String program;
    private String scienceOfficer;
    private String gorCor;
    private BigDecimal piBudget;
    private BigDecimal finalRecommendedBudget;
    private String programManager;
    private String contractGrantsSpecialist;
    private String branchChief;
    private String primeAwardType;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<SubmissionWithSchemaDto> submissions;
    private List<ProjectPersonnel> personnel;
    private List<AwardLinkedFile> linked_files;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getLogNumber() { return logNumber; }
    public void setLogNumber(String logNumber) { this.logNumber = logNumber; }

    public String getAwardNumber() { return awardNumber; }
    public void setAwardNumber(String awardNumber) { this.awardNumber = awardNumber; }

    public String getAwardMechanism() { return awardMechanism; }
    public void setAwardMechanism(String awardMechanism) { this.awardMechanism = awardMechanism; }

    public String getFundingOpportunity() { return fundingOpportunity; }
    public void setFundingOpportunity(String fundingOpportunity) { this.fundingOpportunity = fundingOpportunity; }

    public String getPrincipalInvestigator() { return principalInvestigator; }
    public void setPrincipalInvestigator(String principalInvestigator) { this.principalInvestigator = principalInvestigator; }

    public String getPerformingOrganization() { return performingOrganization; }
    public void setPerformingOrganization(String performingOrganization) { this.performingOrganization = performingOrganization; }

    public String getContractingOrganization() { return contractingOrganization; }
    public void setContractingOrganization(String contractingOrganization) { this.contractingOrganization = contractingOrganization; }

    public String getPeriodOfPerformance() { return periodOfPerformance; }
    public void setPeriodOfPerformance(String periodOfPerformance) { this.periodOfPerformance = periodOfPerformance; }

    public BigDecimal getAwardAmount() { return awardAmount; }
    public void setAwardAmount(BigDecimal awardAmount) { this.awardAmount = awardAmount; }

    public String getProgramOffice() { return programOffice; }
    public void setProgramOffice(String programOffice) { this.programOffice = programOffice; }

    public String getProgram() { return program; }
    public void setProgram(String program) { this.program = program; }

    public String getScienceOfficer() { return scienceOfficer; }
    public void setScienceOfficer(String scienceOfficer) { this.scienceOfficer = scienceOfficer; }

    public String getGorCor() { return gorCor; }
    public void setGorCor(String gorCor) { this.gorCor = gorCor; }

    public BigDecimal getPiBudget() { return piBudget; }
    public void setPiBudget(BigDecimal piBudget) { this.piBudget = piBudget; }

    public BigDecimal getFinalRecommendedBudget() { return finalRecommendedBudget; }
    public void setFinalRecommendedBudget(BigDecimal finalRecommendedBudget) { this.finalRecommendedBudget = finalRecommendedBudget; }

    public String getProgramManager() { return programManager; }
    public void setProgramManager(String programManager) { this.programManager = programManager; }

    public String getContractGrantsSpecialist() { return contractGrantsSpecialist; }
    public void setContractGrantsSpecialist(String contractGrantsSpecialist) { this.contractGrantsSpecialist = contractGrantsSpecialist; }

    public String getBranchChief() { return branchChief; }
    public void setBranchChief(String branchChief) { this.branchChief = branchChief; }

    public String getPrimeAwardType() { return primeAwardType; }
    public void setPrimeAwardType(String primeAwardType) { this.primeAwardType = primeAwardType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<SubmissionWithSchemaDto> getSubmissions() { return submissions; }
    public void setSubmissions(List<SubmissionWithSchemaDto> submissions) { this.submissions = submissions; }

    public List<ProjectPersonnel> getPersonnel() { return personnel; }
    public void setPersonnel(List<ProjectPersonnel> personnel) { this.personnel = personnel; }

    public List<AwardLinkedFile> getLinked_files() { return linked_files; }
    public void setLinked_files(List<AwardLinkedFile> linked_files) { this.linked_files = linked_files; }
}
