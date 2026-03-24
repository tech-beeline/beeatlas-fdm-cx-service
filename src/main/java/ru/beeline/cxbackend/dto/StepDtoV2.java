/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepDtoV2 {

    private List<BIDto> bi = new ArrayList<>();

    private Long id;

    private Long cjId;

    private Integer order;

    private String name;

    private String description;
}
