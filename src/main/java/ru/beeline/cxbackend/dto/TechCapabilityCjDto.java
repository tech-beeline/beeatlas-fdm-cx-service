/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechCapabilityCjDto {

    private Long id;
    private String name;
    @JsonProperty("unique_ident")
    private String uniqueIdent;
    @JsonProperty("b_draft")
    private boolean bDraft;
}
