/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.annotation.ApiStandardErrors;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.dto.BIDto;
import ru.beeline.cxbackend.dto.BiByCjStepDto;
import ru.beeline.cxbackend.dto.CjResponseDto;
import ru.beeline.cxbackend.service.BusinessInteractionService;

import java.util.List;

@RestController
@RequestMapping(value = "/api/cx/v1")

@Tag(
        name = "BI by CjStep",
        description = "Привязка BI к шагам CJ: получить BI по шагу CJ, получить CJ по BI, привязать/отвязать BI к шагу CJ."
)
public class BICJStepController {

    @Autowired
    private BusinessInteractionService businessInteractionService;

    @GetMapping("/product/cj/step/{id}/bi")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "BI по шагу CJ", description = "Возвращает список BI, привязанных к шагу CJ по id шага.")
    public ResponseEntity<List<BIDto>> getBIByCJStep(@PathVariable(value = "id") Long idStep) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(businessInteractionService.getBIByStepId(idStep));
    }

    @GetMapping("/product/cj/step/bi/{id}")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "CJ по BI", description = "Возвращает список CJ, в которых используется BI по id BI.")
    public ResponseEntity<List<CjResponseDto>> getCJsByBiId(@PathVariable(value = "id") Long id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(businessInteractionService.getCJByBIID(id));
    }

    @PutMapping("/product/cj/step/{id}/bi")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Привязка BI к шагу CJ", description = "Создаёт/обновляет привязку BI к шагу CJ.")
    public ResponseEntity<Void> editBIByCJStep(@PathVariable(value = "id") Long idStep,
                                               @RequestBody BiByCjStepDto bi) {
        businessInteractionService.editBIByStepId(idStep, bi);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @DeleteMapping("/product/cj/step/{id_step}/bi/{id}")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Удаление BI из шага CJ", description = "Удаляет связь BI с шагом CJ.")
    public ResponseEntity<Void> DeleteBIByCJStep(@PathVariable(value = "id_step") Long idStep,
                                                 @PathVariable(value = "id") Long idBi) {
        businessInteractionService.deleteBIByStepId(idStep, idBi);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}