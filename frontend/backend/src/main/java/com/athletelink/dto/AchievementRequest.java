package com.athletelink.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class AchievementRequest {
    @NotBlank private String title;
    @NotBlank private String type;
    @NotBlank private String sport;
    @NotBlank private String level;
    @Min(1900) @Max(2100) private Integer year;
    private String academicYear;
    private String institution;
    private String position;
    private String medalAward;
    private String eventDate;
    private String location;
    private String description;
    private String certificateDetails;
    private String certificateUrl;

    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getType(){return type;} public void setType(String v){type=v;}
    public String getSport(){return sport;} public void setSport(String v){sport=v;}
    public String getLevel(){return level;} public void setLevel(String v){level=v;}
    public Integer getYear(){return year;} public void setYear(Integer v){year=v;}
    public String getAcademicYear(){return academicYear;} public void setAcademicYear(String v){academicYear=v;}
    public String getInstitution(){return institution;} public void setInstitution(String v){institution=v;}
    public String getPosition(){return position;} public void setPosition(String v){position=v;}
    public String getMedalAward(){return medalAward;} public void setMedalAward(String v){medalAward=v;}
    public String getEventDate(){return eventDate;} public void setEventDate(String v){eventDate=v;}
    public String getLocation(){return location;} public void setLocation(String v){location=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getCertificateDetails(){return certificateDetails;} public void setCertificateDetails(String v){certificateDetails=v;}
    public String getCertificateUrl(){return certificateUrl;} public void setCertificateUrl(String v){certificateUrl=v;}
}
