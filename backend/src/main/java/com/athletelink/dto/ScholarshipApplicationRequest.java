package com.athletelink.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class ScholarshipApplicationRequest {
    private String category;
    private String categoryTitle;
    private String fullName;
    private String email;
    private String phone;
    private Integer age;
    private String dob;
    private Double familyIncome;
    private Double familyAnnualIncome;
    private Double singleIncome;
    private String parentGuardianName;
    private String defenceRelationship;
    private String supportingPersonName;
    private String currentGrade;
    private String maintainedGrade;
    private String stationeryRequired;
    private String resourcesRequired;
    private String requiredResourcesList;
    private Integer lossYear;
    private String ageProofDocument;
    private String parentIdDocument;
    private String applicantIdDocument;
    private String aadhaarDocument;
    private String defenceIdDocument;
    private Map<String, String> documents;
    private String documentsJson;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCategoryTitle() { return categoryTitle; }
    public void setCategoryTitle(String categoryTitle) { this.categoryTitle = categoryTitle; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }

    public Double getFamilyIncome() {
        if (familyIncome != null) return familyIncome;
        if (familyAnnualIncome != null) return familyAnnualIncome;
        if (singleIncome != null) return singleIncome;
        return null;
    }
    public void setFamilyIncome(Double familyIncome) { this.familyIncome = familyIncome; }
    public Double getFamilyAnnualIncome() { return familyAnnualIncome; }
    public void setFamilyAnnualIncome(Double familyAnnualIncome) { this.familyAnnualIncome = familyAnnualIncome; }
    public Double getSingleIncome() { return singleIncome; }
    public void setSingleIncome(Double singleIncome) { this.singleIncome = singleIncome; }

    public String getParentGuardianName() { return parentGuardianName; }
    public void setParentGuardianName(String parentGuardianName) { this.parentGuardianName = parentGuardianName; }

    public String getDefenceRelationship() { return defenceRelationship; }
    public void setDefenceRelationship(String defenceRelationship) { this.defenceRelationship = defenceRelationship; }

    public String getSupportingPersonName() { return supportingPersonName; }
    public void setSupportingPersonName(String supportingPersonName) { this.supportingPersonName = supportingPersonName; }

    public String getCurrentGrade() { return currentGrade; }
    public void setCurrentGrade(String currentGrade) { this.currentGrade = currentGrade; }

    public String getMaintainedGrade() { return maintainedGrade; }
    public void setMaintainedGrade(String maintainedGrade) { this.maintainedGrade = maintainedGrade; }

    public String getStationeryRequired() { return stationeryRequired; }
    public void setStationeryRequired(String stationeryRequired) { this.stationeryRequired = stationeryRequired; }

    public String getResourcesRequired() { return resourcesRequired; }
    public void setResourcesRequired(String resourcesRequired) { this.resourcesRequired = resourcesRequired; }

    public String getRequiredResourcesList() { return requiredResourcesList; }
    public void setRequiredResourcesList(String requiredResourcesList) { this.requiredResourcesList = requiredResourcesList; }

    public Integer getLossYear() { return lossYear; }
    public void setLossYear(Integer lossYear) { this.lossYear = lossYear; }

    public String getAgeProofDocument() { return ageProofDocument; }
    public void setAgeProofDocument(String ageProofDocument) { this.ageProofDocument = ageProofDocument; }

    public String getParentIdDocument() { return parentIdDocument; }
    public void setParentIdDocument(String parentIdDocument) { this.parentIdDocument = parentIdDocument; }

    public String getApplicantIdDocument() { return applicantIdDocument; }
    public void setApplicantIdDocument(String applicantIdDocument) { this.applicantIdDocument = applicantIdDocument; }

    public String getAadhaarDocument() { return aadhaarDocument; }
    public void setAadhaarDocument(String aadhaarDocument) { this.aadhaarDocument = aadhaarDocument; }

    public String getDefenceIdDocument() { return defenceIdDocument; }
    public void setDefenceIdDocument(String defenceIdDocument) { this.defenceIdDocument = defenceIdDocument; }

    public Map<String, String> getDocuments() {
        Map<String, String> map = new LinkedHashMap<>();
        if (documents != null) map.putAll(documents);
        if (ageProofDocument != null && !ageProofDocument.isBlank()) map.put("ageProof", ageProofDocument);
        if (parentIdDocument != null && !parentIdDocument.isBlank()) map.put("parentGuardianId", parentIdDocument);
        if (applicantIdDocument != null && !applicantIdDocument.isBlank()) map.put("applicantId", applicantIdDocument);
        if (aadhaarDocument != null && !aadhaarDocument.isBlank()) map.put("aadhaarCard", aadhaarDocument);
        if (defenceIdDocument != null && !defenceIdDocument.isBlank()) map.put("defenceId", defenceIdDocument);
        return map;
    }
    public void setDocuments(Map<String, String> documents) { this.documents = documents; }

    public String getDocumentsJson() { return documentsJson; }
    public void setDocumentsJson(String documentsJson) { this.documentsJson = documentsJson; }
}
