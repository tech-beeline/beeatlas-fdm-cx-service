/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.cxbackend.domain.bi.ref.BIChannel;
import ru.beeline.cxbackend.domain.bi.ref.BIFeeling;
import ru.beeline.cxbackend.domain.bi.ref.BIParticipant;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;
import ru.beeline.cxbackend.service.BIReferenceService;

import java.util.List;

@RestController
@RequestMapping(value = "/api/cx/v1/references")
@Api(value = "CX API", tags = "BI References")
public class BIReferenceController {

    @Autowired
    private BIReferenceService biReferenceService;

    @GetMapping("/feelings")
    @ApiOperation(value = "Получение значений справочника чувств", response = List.class)
    public ResponseEntity<List<BIFeeling>> getBIFeelings() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(biReferenceService.getFeelings());
    }

    @GetMapping("/bi_status")
    @ApiOperation(value = "Получение значений справочника статусов", response = List.class)
    public ResponseEntity<List<BIStatus>> getBIStatus() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(biReferenceService.getStatus());
    }

    @GetMapping("/channels")
    @ApiOperation(value = "Получение значений справочника каналов", response = List.class)
    public ResponseEntity<List<BIChannel>> getBIChannels() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(biReferenceService.getChannels());
    }

    @GetMapping("/participants")
    @ApiOperation(value = "Получение значений справочника участников", response = List.class)
    public ResponseEntity<List<BIParticipant>> getBIParticipants() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(biReferenceService.getParticipants());
    }
}