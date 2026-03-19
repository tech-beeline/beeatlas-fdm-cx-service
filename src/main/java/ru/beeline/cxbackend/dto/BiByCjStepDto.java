/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BiByCjStepDto {
    @JsonProperty("id_bi")
    private Long idBi;
    private Long order;
}
