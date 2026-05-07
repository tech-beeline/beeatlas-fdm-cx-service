package ru.beeline.cxbackend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.beeline.cxbackend.dto.alerts.BiAlertDto;
import ru.beeline.cxbackend.dto.alerts.BiStepAlertDto;
import ru.beeline.cxbackend.dto.alerts.CjAlertsDto;
import ru.beeline.cxbackend.dto.alerts.acc.BiAcc;
import ru.beeline.cxbackend.dto.alerts.acc.CjAcc;
import ru.beeline.cxbackend.repository.CJRepository;
import ru.beeline.cxbackend.repository.projection.CjAlertsFlatRow;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CjAlertsService {
    private final CJRepository cjRepository;

    @Transactional(readOnly = true)
    public List<CjAlertsDto> getCjAlerts() {
        List<CjAlertsFlatRow> rows = cjRepository.findCjAlertsFlat();
        if (rows.isEmpty()) {
            return List.of();
        }

        Map<Long, CjAcc> cjAcc = new LinkedHashMap<>();
        for (CjAlertsFlatRow r : rows) {
            Long cjId = r.getCjId();
            if (cjId == null) {
                continue;
            }

            CjAcc acc = cjAcc.computeIfAbsent(cjId, __ -> new CjAcc(
                    cjId,
                    r.getCjName(),
                    r.getCjUniqueIdent(),
                    r.getCjDashboardLink()
            ));

            Long biId = r.getBiId();
            if (biId == null) {
                continue;
            }

            BiAcc biAcc = acc.bi.computeIfAbsent(biId, __ -> new BiAcc(
                    biId,
                    r.getBiUniqueIdent(),
                    r.getBiName(),
                    r.getBiDescr()
            ));

            Integer idStepType = r.getBsIdStepType();
            if (idStepType == null) {
                continue;
            }

            biAcc.steps.add(BiStepAlertDto.builder()
                    .idStepType(idStepType)
                    .uniqueIdent(r.getBsUniqueIdent())
                    .name(r.getBsName())
                    .latency(r.getBsLatency())
                    .rps(r.getBsRps())
                    .errorRate(r.getBsErrorRate())
                    .build());
        }

        return cjAcc.values().stream()
                .map(CjAcc::toDto)
                .collect(Collectors.toList());
    }
}

