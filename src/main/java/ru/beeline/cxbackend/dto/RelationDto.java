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
public class RelationDto {

    private Integer id;
    private Integer userId;
    private String description;
    private Integer tcId;
    private String tcName;
    private String tcCode;
    private Integer productId;
    private String productName;
    private String productAlias;
    private Integer interfaceId;
    private String interfaceName;
    private String interfaceCode;
    private Integer operationId;
    private String operation;
    private Integer order;
}
