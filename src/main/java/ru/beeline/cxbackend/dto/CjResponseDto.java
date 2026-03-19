/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.sql.Date;

import static ru.beeline.cxbackend.utils.Constant.DATE_FORMAT;
import static ru.beeline.cxbackend.utils.Constant.DATE_TIMEZONE;

@Data
@Getter
public class CjResponseDto {

    private Long id;

    private String name;

    @JsonProperty("user_portrait")
    private String userPortrait;

    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date lastModifiedDate;

    private Date deletedDate;

    @JsonFormat(pattern = DATE_FORMAT, timezone = DATE_TIMEZONE)
    private Date createdDate;

    @JsonProperty("draft")
    private Boolean bDraft;

    @JsonProperty("id_user_profile")
    private Long authorId;

    @JsonProperty("id_product")
    private Long idProductExt;

    private String uniqueIdent;

    private Boolean bpmn;

}
