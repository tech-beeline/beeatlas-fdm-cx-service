/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiStepDto {

    private Integer id;
    private String name;
    private Float latency;
    private Float errorRate;
    private Float rps;
    private List<RelationDto> relations;
}
