/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.cxbackend.annotation.ApiStandardErrors;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.domain.bi.ref.BIChannel;
import ru.beeline.cxbackend.domain.bi.ref.BIFeeling;
import ru.beeline.cxbackend.domain.bi.ref.BIParticipant;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;
import ru.beeline.cxbackend.service.BIReferenceService;

import java.util.List;

@RestController
@RequestMapping(value = "/api/cx/v1/references")
@Tag(
        name = "BI References",
        description = "Справочники для BI: чувства, статусы, каналы, участники (для заполнения/отображения карточек BI)."
)
public class BIReferenceController {

    @Autowired
    private BIReferenceService biReferenceService;

    @GetMapping("/feelings")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Справочник BI: чувства", description = "Возвращает справочник чувств для карточки BI.")
    public ResponseEntity<List<BIFeeling>> getBIFeelings() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(biReferenceService.getFeelings());
    }

    @GetMapping("/bi_status")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Справочник BI: статусы", description = "Возвращает справочник статусов BI.")
    public ResponseEntity<List<BIStatus>> getBIStatus() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(biReferenceService.getStatus());
    }

    @GetMapping("/channels")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Справочник BI: каналы", description = "Возвращает справочник каналов BI.")
    public ResponseEntity<List<BIChannel>> getBIChannels() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(biReferenceService.getChannels());
    }

    @GetMapping("/participants")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Справочник BI: участники", description = "Возвращает справочник участников BI.")
    public ResponseEntity<List<BIParticipant>> getBIParticipants() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(biReferenceService.getParticipants());
    }
}