/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.List;


@Data
@Getter
public class CJTagsDto {

    private String name;
    @JsonProperty("user_portrait")
    private String userPortrait;

    @JsonIgnore
    private boolean userPortraitProvided = false;
    @JsonProperty("draft")
    private Boolean bDraft;
    private List<String> tags;

    private Long businessOwner;

    private List<Long> techOwners;

    @JsonIgnore
    private boolean businessOwnerProvided = false;

    @JsonIgnore
    private boolean techOwnersProvided = false;

    @JsonProperty("user_portrait")
    public void setUserPortrait(String userPortrait) {
        this.userPortrait = userPortrait;
        this.userPortraitProvided = true;
    }

    @JsonProperty("businessOwner")
    public void setBusinessOwner(Long businessOwner) {
        this.businessOwner = businessOwner;
        this.businessOwnerProvided = true;
    }

    @JsonProperty("techOwners")
    public void setTechOwners(List<Long> techOwners) {
        this.techOwners = techOwners;
        this.techOwnersProvided = true;
    }

    public boolean isUserPortraitProvided() {
        return userPortraitProvided;
    }

    public boolean isBusinessOwnerProvided() {
        return businessOwnerProvided;
    }

    public boolean isTechOwnersProvided() {
        return techOwnersProvided;
    }
}
