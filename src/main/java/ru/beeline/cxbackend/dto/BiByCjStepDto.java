/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BiByCjStepDto {
    @JsonProperty("id_bi")
    private Long idBi;
    private BigDecimal order;
}
