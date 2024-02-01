package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.domain.bi.ref.BIChannel;
import ru.beeline.cxbackend.domain.bi.ref.BIFeeling;
import ru.beeline.cxbackend.domain.bi.ref.BIParticipant;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;
import ru.beeline.cxbackend.service.BIReferenceService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/cx/v1/references")
@Api(value = "CX API", tags = "BI References")
public class BIReferenceController {

    @Autowired
    private BIReferenceService biReferenceService;

    @GetMapping("/feelings")
    @ApiOperation(value = "Получение значений справочника чувств", response = List.class)
    public ResponseEntity<List<BIFeeling>> getBIFeelings(@RequestHeader("Authorization") String bearerToken) {
        return ResponseEntity.ok(biReferenceService.getFeelings());
    }

    @GetMapping("/bi_status")
    @ApiOperation(value = "Получение значений справочника статусов", response = List.class)
    public ResponseEntity<List<BIStatus>> getBIStatus(@RequestHeader("Authorization") String bearerToken) {
        return ResponseEntity.ok(biReferenceService.getStatus());
    }

    @GetMapping("/channels")
    @ApiOperation(value = "Получение значений справочника каналов", response = List.class)
    public ResponseEntity<List<BIChannel>> getBIChannels(@RequestHeader("Authorization") String bearerToken) {
        return ResponseEntity.ok(biReferenceService.getChannels());
    }

    @GetMapping("/participants")
    @ApiOperation(value = "Получение значений справочника участников", response = List.class)
    public ResponseEntity<List<BIParticipant>> getBIParticipants(@RequestHeader("Authorization") String bearerToken) {
        return ResponseEntity.ok(biReferenceService.getParticipants());
    }

}