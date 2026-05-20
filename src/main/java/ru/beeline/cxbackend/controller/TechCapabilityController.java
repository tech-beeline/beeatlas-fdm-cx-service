/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.cxbackend.annotation.ApiStandardErrors;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.dto.TechCapabilityCjDto;
import ru.beeline.cxbackend.service.TechCapabilityCjService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(
        name = "Tech Capability",
        description = "Связь технических возможностей (TC) с Customer Journey через операции продукта и шаги BI/CJ."
)
@RequestMapping("/api/cx/v1")
public class TechCapabilityController {

    private final TechCapabilityCjService techCapabilityCjService;

    @GetMapping(value = "/tech-capability/{id}/cj", produces = MediaType.APPLICATION_JSON_VALUE)
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "CJ по технической возможности",
            description = """
                    Возвращает CJ, связанные с TC `id` или с операциями реализации этой TC (данные продукта), \
                    по цепочке bi_steps_relations → bi_steps → bi_in_cj_step → cj_steps → cj."""
    )
    public ResponseEntity<List<TechCapabilityCjDto>> listCjByTechCapability(@PathVariable("id") Integer id) {
        return ResponseEntity.status(HttpStatus.OK).body(techCapabilityCjService.listCjByTechCapability(id));
    }
}
