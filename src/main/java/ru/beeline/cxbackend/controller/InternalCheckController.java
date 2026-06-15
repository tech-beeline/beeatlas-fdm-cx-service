/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.repository.BusinessInteractionRepository;
import ru.beeline.cxbackend.repository.CJRepository;
import ru.beeline.cxbackend.repository.CJStepRepository;

import java.util.List;
import java.util.Map;

@RestController
public class InternalCheckController {

    @Autowired
    private CJStepRepository cjStepRepository;

    @Autowired
    private CJRepository cjRepository;

    @Autowired
    private BusinessInteractionRepository biRepository;

    @GetMapping("/api/cx/v1/internal/check/cj-step/{id}/product-member")
    public ResponseEntity<Map<String, Boolean>> checkCjStepProductMember(
            @PathVariable Long id,
            @RequestParam List<Long> productIds) {
        return cjStepRepository.findById(id)
                .flatMap(step -> cjRepository.findById(step.getCjId()))
                .map(cj -> productIds.contains(cj.getIdProductExt()))
                .map(hasAccess -> ResponseEntity.ok(Map.of("hasAccess", hasAccess)))
                .orElseThrow(() -> new NotFoundException("CJ step not found: " + id));
    }

    @GetMapping("/api/cx/v1/internal/check/bi/{id}/product-member")
    public ResponseEntity<Map<String, Boolean>> checkBiProductMember(
            @PathVariable Long id,
            @RequestParam List<Long> productIds) {
        return biRepository.findByIdAndDeletedDateIsNull(id)
                .map(bi -> !bi.isDraft() || productIds.contains(bi.getProductId()))
                .map(hasAccess -> ResponseEntity.ok(Map.of("hasAccess", hasAccess)))
                .orElseThrow(() -> new NotFoundException("BI not found: " + id));
    }

    @GetMapping("/api/cx/v1/internal/check/bi/{id}/edit-access")
    public ResponseEntity<Map<String, Boolean>> checkBiEditAccess(
            @PathVariable Long id,
            @RequestParam List<Long> productIds) {
        return biRepository.findByIdAndDeletedDateIsNull(id)
                .map(bi -> productIds.contains(bi.getProductId()))
                .map(hasAccess -> ResponseEntity.ok(Map.of("hasAccess", hasAccess)))
                .orElseThrow(() -> new NotFoundException("BI not found: " + id));
    }

    @GetMapping("/api/cx/v1/internal/check/cj/{id}/product-member")
    public ResponseEntity<Map<String, Boolean>> checkCjProductMember(
            @PathVariable Long id,
            @RequestParam List<Long> productIds) {
        return cjRepository.findById(id)
                .filter(cj -> cj.getDeletedDate() == null)
                .map(cj -> !cj.isBDraft() || productIds.contains(cj.getIdProductExt()))
                .map(hasAccess -> ResponseEntity.ok(Map.of("hasAccess", hasAccess)))
                .orElseThrow(() -> new NotFoundException("CJ not found: " + id));
    }

    @GetMapping("/api/cx/v1/internal/check/cj/{id}/edit-access")
    public ResponseEntity<Map<String, Boolean>> checkCjEditAccess(
            @PathVariable Long id,
            @RequestParam List<Long> productIds) {
        return cjRepository.findById(id)
                .filter(cj -> cj.getDeletedDate() == null)
                .map(cj -> productIds.contains(cj.getIdProductExt()))
                .map(hasAccess -> ResponseEntity.ok(Map.of("hasAccess", hasAccess)))
                .orElseThrow(() -> new NotFoundException("CJ not found: " + id));
    }
}
