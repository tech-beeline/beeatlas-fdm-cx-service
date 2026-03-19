/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
public class StepDto {

    private List<BIDto> bi = new ArrayList<>();

    private Long id;

    @JsonProperty("id_cj")
    private Long cjId;

    private Integer order;

    private String name;

    private String description;
}
