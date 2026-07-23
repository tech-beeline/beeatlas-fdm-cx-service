/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.cxbackend.annotation.ApiStandardErrors;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.dto.bistep.BiStepContextDto;
import ru.beeline.cxbackend.service.BiStepContextService;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "BI Step",
        description = "Контекст шага BI: родительский BI и Customer Journey, в которых этот BI используется."
)
@RequestMapping("/api/cx/v1")
public class BiStepController {

    private final BiStepContextService biStepContextService;

    @GetMapping(value = "/bi-step/{code}")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "Контекст bi-step по code",
            description = "Получения контекста bi-step по его коду (unique_ident): к какому BI принадлежит шаг " +
                    "и в каких CJ этот BI используется. Цепочка bi_steps → business_iteraction → bi_in_cj_step → cj_steps → cj.")
    @ApiResponse(
            responseCode = "200",
            description = "Контекст bi-step",
            content = @Content(schema = @Schema(implementation = BiStepContextDto.class)))
    public ResponseEntity<BiStepContextDto> getByCode(@PathVariable("code") String code) {
        return ResponseEntity.status(HttpStatus.OK).body(biStepContextService.getByCode(code));
    }
}
