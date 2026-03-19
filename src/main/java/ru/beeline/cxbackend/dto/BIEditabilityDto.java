/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.sun.istack.NotNull;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;


@Data
public class BIEditabilityDto {

    @NotNull
    @ApiModelProperty(required = true)
    private Boolean editability;
}
