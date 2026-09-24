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
        Map<String, List<String>> e2eCodesByBiStepUid = buildBiStepToE2eCodesMap(e2eList);
        if (e2eCodesByBiStepUid.isEmpty()) {
            log.info("CJ e2e: нет e2e с bi_step_code");
            return List.of();
        }
        List<CjAlertsFlatRow> rows = cjRepository.findCjAlertsFlat();
        if (rows.isEmpty()) {
            log.info("CJ e2e: локальные CJ отсутствуют");
            return List.of();
        }
        List<CjE2eDto> result = buildTree(rows, e2eCodesByBiStepUid);
        log.info("CJ e2e: завершён, cjCount={}, matchedBiStepCodes={}",
                result.size(), e2eCodesByBiStepUid.size());
        return result;
    }

    private Map<String, List<String>> buildBiStepToE2eCodesMap(List<E2eCardDto> e2eList) {
        Map<String, Set<String>> codesByBiStep = new LinkedHashMap<>();
        for (E2eCardDto e2e : e2eList) {
            if (e2e == null || !StringUtils.hasText(e2e.getBiStepCode()) || !StringUtils.hasText(e2e.getCode())) {
                continue;
            }
            codesByBiStep
                    .computeIfAbsent(e2e.getBiStepCode().trim(), k -> new LinkedHashSet<>())
                    .add(e2e.getCode());
        }
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : codesByBiStep.entrySet()) {
            List<String> sortedCodes = new ArrayList<>(entry.getValue());
            sortedCodes.sort(String.CASE_INSENSITIVE_ORDER);
            result.put(entry.getKey(), sortedCodes);
        }
        return result;
    }

    private List<CjE2eDto> buildTree(List<CjAlertsFlatRow> rows, Map<String, List<String>> e2eCodesByBiStepUid) {
        Map<Long, CjE2eAcc> cjAcc = new LinkedHashMap<>();
        for (CjAlertsFlatRow row : rows) {
            Long cjId = row.getCjId();
            Long biId = row.getBiId();
            String biStepUid = row.getBsUniqueIdent();
            if (cjId == null || biId == null || !StringUtils.hasText(biStepUid)) {
                continue;
            }
            List<String> e2eCodes = e2eCodesByBiStepUid.get(biStepUid);
            if (e2eCodes == null || e2eCodes.isEmpty()) {
                continue;
            }
            CjE2eAcc cj = cjAcc.computeIfAbsent(cjId, id -> new CjE2eAcc(id, row.getCjUniqueIdent(), row.getCjName()));
            BiE2eAcc bi = cj.bi.computeIfAbsent(biId, id -> new BiE2eAcc(id, row.getBiUniqueIdent(), row.getBiName()));
            if (!bi.stepUids.contains(biStepUid)) {
                bi.stepUids.add(biStepUid);
                bi.steps.add(BiStepE2eDto.builder()
                        .uid(biStepUid)
                        .name(row.getBsName())
                        .e2eCodes(new ArrayList<>(e2eCodes))
                        .build());
            }
        }
        return cjAcc.values().stream().map(CjE2eAcc::toDto).collect(Collectors.toList());
    }
}
