/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BIInCJStep;
import ru.beeline.cxbackend.domain.bi.BiStep;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.bistep.BiRefDto;
import ru.beeline.cxbackend.dto.bistep.BiStepContextDto;
import ru.beeline.cxbackend.dto.bistep.BiStepRefDto;
import ru.beeline.cxbackend.dto.bistep.CjRefDto;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.repository.BIInCJStepRepository;
import ru.beeline.cxbackend.repository.BiStepRepository;
import ru.beeline.cxbackend.repository.BusinessInteractionRepository;
import ru.beeline.cxbackend.repository.CJRepository;
import ru.beeline.cxbackend.repository.CJStepRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiStepContextService {

    private final BiStepRepository biStepRepository;
    private final BusinessInteractionRepository businessInteractionRepository;
    private final BIInCJStepRepository biInCJStepRepository;
    private final CJStepRepository cjStepRepository;
    private final CJRepository cjRepository;

    @Transactional(readOnly = true)
    public BiStepContextDto getByCode(String code) {
        log.info("getByCode: начало, code={}", code);
        BiStep biStep = biStepRepository.findByUniqueIdent(code)
                .orElseThrow(() -> new NotFoundException("bi-step с code = " + code + " не найден"));
        Long biId = biStep.getBiId();
        if (biId == null) {
            throw new NotFoundException("BI для bi-step с code = " + code + " не найден");
        }
        BI bi = businessInteractionRepository.findByIdAndDeletedDateIsNull(biId)
                .orElseThrow(() -> new NotFoundException("BI с id = " + biId + " не найден"));
        List<CjRefDto> cjList = findCjByBiId(bi.getId());
        log.info("BiStep context: завершён, code={}, biId={}, cjCount={}", code, bi.getId(), cjList.size());
        return BiStepContextDto.builder()
                .biStep(toBiStepRef(biStep))
                .bi(toBiRef(bi))
                .cj(cjList)
                .build();
    }

    private List<CjRefDto> findCjByBiId(Long biId) {
        List<BIInCJStep> links = biInCJStepRepository.findBIInCJStepsByBiId(biId);
        if (links.isEmpty()) {
            return List.of();
        }
        Set<Long> cjStepIds = links.stream()
                .map(BIInCJStep::getCjStepId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (cjStepIds.isEmpty()) {
            return List.of();
        }
        Set<Long> cjIds = cjStepRepository.findAllByIdIn(cjStepIds).stream()
                .map(CJStep::getCjId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (cjIds.isEmpty()) {
            return List.of();
        }
        return cjRepository.findAllByIdInAndDeletedDateIsNull(cjIds).stream()
                .sorted(Comparator.comparing(CJ::getId))
                .map(this::toCjRef)
                .collect(Collectors.toList());
    }

    private BiStepRefDto toBiStepRef(BiStep biStep) {
        return BiStepRefDto.builder()
                .id(biStep.getId())
                .uid(biStep.getUniqueIdent())
                .name(biStep.getName())
                .build();
    }

    private BiRefDto toBiRef(BI bi) {
        return BiRefDto.builder()
                .id(bi.getId())
                .uid(bi.getUniqueIdent())
                .name(bi.getName())
                .build();
    }

    private CjRefDto toCjRef(CJ cj) {
        return CjRefDto.builder()
                .id(cj.getId())
                .uid(cj.getUniqueIdent())
                .name(cj.getName())
                .bDraft(cj.isBDraft())
                .build();
    }
}
