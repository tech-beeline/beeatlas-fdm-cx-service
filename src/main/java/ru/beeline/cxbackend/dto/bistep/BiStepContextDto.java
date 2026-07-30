/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto.bistep;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiStepContextDto {

    private BiStepRefDto biStep;
    private BiRefDto bi;
    @Builder.Default
    private List<CjRefDto> cj = new ArrayList<>();
}
