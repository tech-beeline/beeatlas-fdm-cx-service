package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.sql.Date;
import java.util.List;


@Data
@Getter
public class CJFullDto {

    private Long id;

    private String name;

    @JsonProperty("user_portrait")
    private String userPortrait;

    @JsonProperty("last_updated")
    private Date lastUpdated;

    @JsonProperty("draft")
    private Boolean bDraft;

    @JsonProperty("id_user_profile")
    private Long authorId;

    @JsonProperty("id_product")
    private String idProductExt;

    private List<StepDto> steps;

}

