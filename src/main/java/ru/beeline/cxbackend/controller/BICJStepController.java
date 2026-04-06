/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.dto.BIDto;
import ru.beeline.cxbackend.dto.BiByCjStepDto;
import ru.beeline.cxbackend.dto.CjResponseDto;
import ru.beeline.cxbackend.service.BusinessInteractionService;

import java.util.List;

@RestController
@RequestMapping(value = "/api/cx/v1")

@Api(value = "BI by CjStep API", tags = "BI by CjStep")
public class BICJStepController {

    @Autowired
    private BusinessInteractionService businessInteractionService;

    @GetMapping("/product/cj/step/{id}/bi")
    @ApiOperation(value = "Получение привяязанных BI по шагу CJ", response = List.class)
    public ResponseEntity<List<BIDto>> getBIByCJStep(@PathVariable(value = "id") Long idStep) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(businessInteractionService.getBIByStepId(idStep));
    }

    @GetMapping("/product/cj/step/bi/{id}")
    @ApiOperation(value = "Получение коллекции CJ по используемому в них BI", response = List.class)
    public ResponseEntity<List<CjResponseDto>> getCJsByBiId(@PathVariable(value = "id") Long id) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(businessInteractionService.getCJByBIID(id));
    }

    @PutMapping("/product/cj/step/{id}/bi")
    @ApiOperation(value = "Привязка BI к шагу CJ")
    public ResponseEntity editBIByCJStep(@PathVariable(value = "id") Long idStep,
                                         @RequestBody BiByCjStepDto bi) {
        businessInteractionService.editBIByStepId(idStep, bi);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }

    @DeleteMapping("/product/cj/step/{id_step}/bi/{id}")
    @ApiOperation(value = "Удаление BI из шага CJ")
    public ResponseEntity DeleteBIByCJStep(@PathVariable(value = "id_step") Long idStep,
                                           @PathVariable(value = "id") Long idBi) {
        businessInteractionService.deleteBIByStepId(idStep, idBi);
        return ResponseEntity
                .status(HttpStatus.OK)
                .build();
    }
}