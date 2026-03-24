/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.CjStepDto;
import ru.beeline.cxbackend.dto.CjStepFullDto;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.mapper.CjStepMapper;
import ru.beeline.cxbackend.repository.BIInCJStepRepository;
import ru.beeline.cxbackend.repository.CJRepository;
import ru.beeline.cxbackend.repository.CJStepRepository;

import java.sql.Date;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static ru.beeline.cxbackend.controller.RequestContext.getUserPermissions;
import static ru.beeline.cxbackend.controller.RequestContext.getUserProducts;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;

@Service
public class CJStepService {

    @Autowired
    private CJStepRepository cjStepRepository;

    @Autowired
    private CJRepository cjRepository;

    @Autowired
    private CjStepMapper cjStepMapper;

    @Autowired
    private BIInCJStepRepository biInCJStepRepository;

    public List<CjStepFullDto> getStepByCJId(Long id) {
        validateAccessProduct(getUserPermissions(), getUserProducts(), cjRepository.findById(id).get());
        List<CJStep> cjStepList = cjStepRepository.findAllByCjId(id).stream()
                .sorted(Comparator.comparing(CJStep::getOrder)).toList();
        return cjStepList.stream().map(cjStep -> cjStepMapper.cjStepToSjStepFullDto(cjStep)).toList();
    }

    @Transactional
    public Object addStep(Long id, CjStepDto cjStepDto) {
        CJStep cjStep = CJStep.builder()
                .name(cjStepDto.getName())
                .order(cjStepDto.getOrder())
                .description(cjStepDto.getDescription())
                .cjId(id)
                .build();

        List<CJStep> existSteps = cjStepRepository.findAllByCjId(id);

        if (!existSteps.isEmpty()) {
            Optional<CJ> currentCJ = cjRepository.findById(existSteps.get(0).getCjId());
            if (currentCJ.isPresent() && !currentCJ.get().isBDraft()) {
                throw new RuntimeException("CJ находится в статусе Опубликован. Добавление шага");
            }

            existSteps = existSteps.stream()
                    .filter(step -> step.getOrder() >= cjStepDto.getOrder())
                    .peek(step -> step.setOrder(step.getOrder() + 1))
                    .collect(Collectors.toList());
        }
        cjStepRepository.saveAllAndFlush(existSteps);
        cjStepRepository.saveAndFlush(cjStep);
        return cjStepMapper.cjStepToSjStepFullDto(cjStep);
    }

    public CjStepFullDto getStepFullDto(Long id) {
        CJStep cjStep = getStepById(id);
        return cjStepMapper.cjStepToSjStepFullDto(cjStep);
    }

    public CJStep getStepById(Long id) {
        CJStep cjStep = cjStepRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Step with id " + id + " does not exist"));

        validateAccessProduct(getUserPermissions(), getUserProducts(), cjRepository.findById(cjStep.getCjId()).get());

        return cjStep;


    }

    @Transactional
    public CjStepFullDto updateStep(CJStep cjStep, CjStepDto cjStepDto) {
        Optional<CJ> currentCJ = cjRepository.findById(cjStep.getCjId());
        if (currentCJ.isPresent() && !currentCJ.get().isBDraft()) {
            throw new RuntimeException("CJ находится в статусе Опубликован. Изменение невозможно");
        }
        if (cjStepDto.getName() != null) {
            cjStep.setName(cjStepDto.getName());
        }
        if (cjStepDto.getOrder() != null) {
            if (!Objects.equals(cjStep.getOrder(), cjStepDto.getOrder())) {
                CJStep stepForSwap = cjStepRepository.findByCjIdAndOrder(cjStep.getCjId(), cjStepDto.getOrder());
                if (Objects.nonNull(stepForSwap)) {
                    stepForSwap.setOrder(cjStep.getOrder());
                    cjStepRepository.save(stepForSwap);
                }
            }
            cjStep.setOrder(cjStepDto.getOrder());
            cjStep.setDescription(cjStepDto.getDescription());
        }
        cjStep = cjStepRepository.save(cjStep);
        CJ cj = cjRepository.findById(cjStep.getCjId()).get();
        cj.setLastModifiedDate(new Date(System.currentTimeMillis()));
        cjRepository.save(cj);
        return cjStepMapper.cjStepToSjStepFullDto(cjStep);
    }

    @Transactional
    public void deleteStep(CJStep cjStep) {
        Optional<CJ> currentCJ = cjRepository.findById(cjStep.getCjId());
        if (currentCJ.isPresent() && !currentCJ.get().isBDraft()) {
            throw new RuntimeException("CJ находится в статусе Опубликован. Удаление невозможно");
        }
        biInCJStepRepository.deleteAllByCjStepId(cjStep.getId());
        cjStepRepository.delete(cjStep);
        List<CJStep> existSteps = cjStepRepository.findAllByCjId(cjStep.getCjId());
        if (!existSteps.isEmpty()) {
            existSteps = existSteps.stream()
                    .filter(step -> step.getOrder() > cjStep.getOrder())
                    .peek(step -> step.setOrder(step.getOrder() - 1))
                    .collect(Collectors.toList());
        }
        cjStepRepository.saveAllAndFlush(existSteps);
    }
}
