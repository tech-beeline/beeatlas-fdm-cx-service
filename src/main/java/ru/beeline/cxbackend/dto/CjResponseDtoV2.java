/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.sql.Date;
import java.util.HashSet;
import java.util.Set;

import static ru.beeline.cxbackend.utils.Constant.DATE_FORMAT;
import static ru.beeline.cxbackend.utils.Constant.DATE_TIMEZONE;

@Data
@Getter
public class CjResponseDtoV2 {

    private Long id;

    private String name;

    private String userPortrait;

    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date lastModifiedDate;

    private Date deletedDate;

    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date createdDate;

    @JsonProperty("draft")
    private Boolean bDraft;

    @JsonProperty("productId")
    private Long idProductExt;

    private String dashboardLink;

    private String uniqueIdent;

    private Boolean bpmn;

    private Set<String> tags = new HashSet<>();
}
