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

import java.util.List;
import java.util.Map;

@RestController
public class InternalCheckController {

    @Autowired
    private BusinessInteractionRepository biRepository;

    @GetMapping("/api/cx/v1/internal/check/bi/{id}/product-member")
    public ResponseEntity<Map<String, Boolean>> checkBiProductMember(
            @PathVariable Long id,
            @RequestParam List<Long> productIds) {
        return biRepository.findByIdAndDeletedDateIsNull(id)
                .map(bi -> bi.getProductId() == null || !bi.isDraft() || productIds.contains(bi.getProductId()))
                .map(hasAccess -> ResponseEntity.ok(Map.of("hasAccess", hasAccess)))
                .orElseThrow(() -> new NotFoundException("BI not found: " + id));
    }

    @GetMapping("/api/cx/v1/internal/check/bi/{id}/edit-access")
    public ResponseEntity<Map<String, Boolean>> checkBiEditAccess(
            @PathVariable Long id,
            @RequestParam List<Long> productIds) {
        return biRepository.findByIdAndDeletedDateIsNull(id)
                .map(bi -> bi.getProductId() == null || productIds.contains(bi.getProductId()))
                .map(hasAccess -> ResponseEntity.ok(Map.of("hasAccess", hasAccess)))
                .orElseThrow(() -> new NotFoundException("BI not found: " + id));
    }

}
