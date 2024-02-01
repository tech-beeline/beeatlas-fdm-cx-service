package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;


@Data
@Getter
public class CJDto {

    private String name;
    @JsonProperty("user_portrait")
    private String userPortrait;
    @JsonProperty("draft")
    private Boolean bDraft;

}
