/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.beeline.cxbackend.client.ProductClient;
import ru.beeline.cxbackend.dto.e2e.BiStepE2eDto;
import ru.beeline.cxbackend.dto.e2e.CjE2eDto;
import ru.beeline.cxbackend.dto.e2e.acc.BiE2eAcc;
import ru.beeline.cxbackend.dto.e2e.acc.CjE2eAcc;
import ru.beeline.cxbackend.dto.product.E2eCardDto;
import ru.beeline.cxbackend.repository.CJRepository;
import ru.beeline.cxbackend.repository.projection.CjAlertsFlatRow;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CjE2eService {

    private final ProductClient productClient;
    private final CJRepository cjRepository;

    @Transactional(readOnly = true)
    public List<CjE2eDto> getCjLinkedToE2e() {
        log.info("getCjLinkedToE2e e2e: начало");
        List<E2eCardDto> e2eList = productClient.getE2eWithBiStep();
        if (e2eList == null || e2eList.isEmpty()) {
            log.info("CJ e2e: список e2e из product пуст");
            return List.of();
        }
        Map<String, String> e2eCodeByBiStepUid = buildBiStepToE2eCodeMap(e2eList);
        if (e2eCodeByBiStepUid.isEmpty()) {
            log.info("CJ e2e: нет e2e с bi_step_code");
            return List.of();
        }
        List<CjAlertsFlatRow> rows = cjRepository.findCjAlertsFlat();
        if (rows.isEmpty()) {
            log.info("CJ e2e: локальные CJ отсутствуют");
            return List.of();
        }
        List<CjE2eDto> result = buildTree(rows, e2eCodeByBiStepUid);
        log.info("CJ e2e: завершён, cjCount={}, matchedBiStepCodes={}",
                result.size(), e2eCodeByBiStepUid.size());
        return result;
    }

    private Map<String, String> buildBiStepToE2eCodeMap(List<E2eCardDto> e2eList) {
        Map<String, String> map = new LinkedHashMap<>();
        for (E2eCardDto e2e : e2eList) {
            if (e2e == null || !StringUtils.hasText(e2e.getBiStepCode()) || !StringUtils.hasText(e2e.getCode())) {
                continue;
            }
            map.putIfAbsent(e2e.getBiStepCode().trim(), e2e.getCode());
        }
        return map;
    }

    private List<CjE2eDto> buildTree(List<CjAlertsFlatRow> rows, Map<String, String> e2eCodeByBiStepUid) {
        Map<Long, CjE2eAcc> cjAcc = new LinkedHashMap<>();
        for (CjAlertsFlatRow row : rows) {
            Long cjId = row.getCjId();
            Long biId = row.getBiId();
            String biStepUid = row.getBsUniqueIdent();
            if (cjId == null || biId == null || !StringUtils.hasText(biStepUid)) {
                continue;
            }
            String e2eCode = e2eCodeByBiStepUid.get(biStepUid);
            if (e2eCode == null) {
                continue;
            }
            CjE2eAcc cj = cjAcc.computeIfAbsent(cjId, id -> new CjE2eAcc(id, row.getCjUniqueIdent(), row.getCjName()));
            BiE2eAcc bi = cj.bi.computeIfAbsent(biId, id -> new BiE2eAcc(id, row.getBiUniqueIdent(), row.getBiName()));
            if (!bi.stepUids.contains(biStepUid)) {
                bi.stepUids.add(biStepUid);
                bi.steps.add(BiStepE2eDto.builder()
                        .uid(biStepUid)
                        .name(row.getBsName())
                        .e2eCode(e2eCode)
                        .build());
            }
        }
        return cjAcc.values().stream().map(CjE2eAcc::toDto).collect(Collectors.toList());
    }
}

