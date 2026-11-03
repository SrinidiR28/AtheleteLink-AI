package com.athletelink.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TrialApplicationRequest {
    @NotBlank private String athleteName;
    @NotBlank @Email private String email;
    @NotBlank private String phone;
    @NotBlank private String location;
    @NotBlank private String sport;
    @NotBlank private String position;
    @NotNull @Min(12) @Max(25) private Integer age;
    @NotBlank private String institution;
    private String experience;
    private String achievements;
    @NotBlank private String introduction;
    private String skills;
    private String additionalInfo;

    public String getAthleteName(){return athleteName;} public void setAthleteName(String v){athleteName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
    public String getLocation(){return location;} public void setLocation(String v){location=v;}
    public String getSport(){return sport;} public void setSport(String v){sport=v;}
    public String getPosition(){return position;} public void setPosition(String v){position=v;}
    public Integer getAge(){return age;} public void setAge(Integer v){age=v;}
    public String getInstitution(){return institution;} public void setInstitution(String v){institution=v;}
    public String getExperience(){return experience;} public void setExperience(String v){experience=v;}
    public String getAchievements(){return achievements;} public void setAchievements(String v){achievements=v;}
    public String getIntroduction(){return introduction;} public void setIntroduction(String v){introduction=v;}
    public String getSkills(){return skills;} public void setSkills(String v){skills=v;}
    public String getAdditionalInfo(){return additionalInfo;} public void setAdditionalInfo(String v){additionalInfo=v;}
}
