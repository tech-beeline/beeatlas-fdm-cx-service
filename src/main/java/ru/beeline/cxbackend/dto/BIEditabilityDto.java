/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.sun.istack.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


@Data
public class BIEditabilityDto {

    @NotNull
    @Schema(required = true)
    private Boolean editability;
}
