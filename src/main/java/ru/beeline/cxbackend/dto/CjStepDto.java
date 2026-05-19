/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CjStepDto {
    private String name;
    private BigDecimal order;
    private String description;
}
