/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.List;

import static ru.beeline.cxbackend.utils.Constant.DATE_FORMAT;
import static ru.beeline.cxbackend.utils.Constant.DATE_TIMEZONE;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CJFullDtoV2 {

    private Long id;

    private String name;

    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date lastModifiedDate;

    @JsonProperty("draft")
    private Boolean bDraft;

    private Long productId;

    private String dashboardLink;

    private List<StepDtoV2> steps;

    private String userPortrait;

    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date createdDate;

    private AuthorDto author;

    private String uniqueIdent;

    private Boolean bpmn;

    private List<String> tags;
}
