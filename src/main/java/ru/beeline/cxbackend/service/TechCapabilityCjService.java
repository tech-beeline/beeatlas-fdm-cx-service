/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.cxbackend.client.ProductClient;
import ru.beeline.cxbackend.domain.bi.BIInCJStep;
import ru.beeline.cxbackend.domain.bi.BiStep;
import ru.beeline.cxbackend.domain.bi.BiStepRelation;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.TechCapabilityCjDto;
import ru.beeline.cxbackend.dto.product.ProductOperationByTcItemDto;
import ru.beeline.cxbackend.repository.*;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechCapabilityCjService {

    private final ProductClient productClient;
    private final BiStepRelationRepository biStepRelationRepository;
    private final BiStepRepository biStepRepository;
    private final BIInCJStepRepository biInCJStepRepository;
    private final CJStepRepository cjStepRepository;
    private final CJRepository cjRepository;

    @Transactional
    public List<TechCapabilityCjDto> listCjByTechCapability(Integer tcId) {
        log.info("Начало listCjByTechCapability для tcId: {}", tcId);
        int tcIdInt = Math.toIntExact(tcId);
        List<Integer> operationIds = productClient.getOperationsByTechCapability(tcId).stream()
                .map(ProductOperationByTcItemDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        log.info("Найдено {} operationIds для tcId: {}", operationIds.size(), tcId);
        List<BiStepRelation> biStepRelations = biStepRelationRepository.findAllByTcId(tcIdInt);
        Set<Integer> biStepIds = biStepRelations.stream()
                .map(BiStepRelation::getBiStepId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        log.info("Найдено {} biStepIds по tcId: {}", biStepIds.size(), tcId);
        if (!operationIds.isEmpty()) {
            List<BiStepRelation> biStepRelationList = biStepRelationRepository.findAllByOperationIdIn(operationIds);
            Set<Integer> operationBiStepIds = biStepRelationList.stream().map(BiStepRelation::getBiStepId).collect(Collectors.toSet());
            log.info("Найдено {} biStepIds по operationIds: {}", operationBiStepIds.size(), operationIds.size());
            biStepIds.addAll(operationBiStepIds);
            log.debug("Всего biStepIds после объединения: {}", biStepIds.size());
        }
        if (biStepIds.isEmpty()) {
            log.warn("Не найдено biStepIds для tcId: {}, возвращаем пустой список", tcId);
            return List.of();
        }
        List<BiStep> biSteps = biStepRepository.findAllByIdIn(biStepIds);
        Set<Long> biIds = biSteps.stream().map(BiStep::getBiId).collect(Collectors.toSet());
        log.info("Найдено {} biIds для biStepIds: {}", biIds.size(), biStepIds.size());
        if (biIds.isEmpty()) {
            log.warn("Не найдено biIds для tcId: {}, возвращаем пустой список", tcId);
            return List.of();
        }
        List<BIInCJStep> biInCJStepList = biInCJStepRepository.findAllByBiIdIn(biIds);
        Set<Long> cjStepIds = biInCJStepList.stream().map(BIInCJStep::getCjStepId).collect(Collectors.toSet());
        log.info("Найдено {} cjStepIds для biIds: {}", cjStepIds.size(), biIds.size());
        if (cjStepIds.isEmpty()) {
            log.warn("Не найдено cjStepIds для tcId: {}, возвращаем пустой список", tcId);
            return List.of();
        }
        List<CJStep> cjSteps = cjStepRepository.findAllByIdIn(cjStepIds);
        Set<Long> cjIds = cjSteps.stream().map(CJStep::getCjId).collect(Collectors.toSet());
        log.info("Найдено {} cjIds для cjStepIds: {}", cjIds.size(), cjStepIds.size());
        if (cjIds.isEmpty()) {
            log.warn("Не найдено cjIds для tcId: {}, возвращаем пустой список", tcId);
            return List.of();
        }
        List<TechCapabilityCjDto> result = cjRepository.findAllByIdInAndDeletedDateIsNull(cjIds).stream()
                .sorted(Comparator.comparing(CJ::getId))
                .map(this::toDto)
                .collect(Collectors.toList());
        log.info("Завершен listCjByTechCapability для tcId: {}, размер результата: {}", tcId, result.size());
        return result;
    }

    private TechCapabilityCjDto toDto(CJ cj) {
        log.debug("Преобразование CJ в DTO: id={}, name={}, uniqueIdent={}, isDraft={}",
                cj.getId(), cj.getName(), cj.getUniqueIdent(), cj.isBDraft());
        return new TechCapabilityCjDto(cj.getId(), cj.getName(), cj.getUniqueIdent(), cj.isBDraft());
    }
}
