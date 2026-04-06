/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import lombok.Data;

@Data
public class CjStepDto {
    private String name;
    private Integer order;
    private String description;
}
