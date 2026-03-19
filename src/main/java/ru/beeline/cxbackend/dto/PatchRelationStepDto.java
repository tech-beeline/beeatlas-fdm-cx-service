/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatchRelationStepDto {

    private Integer id;
    private String description;
    private Integer productId;
    private Integer tcId;
    private Integer operationId;
    private Integer interfaceId;
}
