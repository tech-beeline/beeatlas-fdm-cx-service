/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

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
    private boolean userPortraitProvided = false;
    @JsonProperty("draft")
    private Boolean bDraft;
    private List<String> tags;

    @JsonProperty("user_portrait")
    public void setUserPortrait(String userPortrait) {
        this.userPortrait = userPortrait;
        this.userPortraitProvided = true;
    }

    public boolean isUserPortraitProvided() {
        return userPortraitProvided;
    }
}
