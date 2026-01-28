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
    @JsonProperty("draft")
    private Boolean bDraft;
    private List<String> tags;

}
