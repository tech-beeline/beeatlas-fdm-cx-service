package ru.beeline.cxbackend.dto.alerts.acc;

import ru.beeline.cxbackend.dto.alerts.CjAlertsDto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class CjAcc {
    public final Long id;
    public final String name;
    public final String uniqueIdent;
    public final String dashboardLink;
    public final Map<Long, BiAcc> bi = new LinkedHashMap<>();

    public CjAcc(Long id, String name, String uniqueIdent, String dashboardLink) {
        this.id = id;
        this.name = name;
        this.uniqueIdent = uniqueIdent;
        this.dashboardLink = dashboardLink;
    }

    public CjAlertsDto toDto() {
        return CjAlertsDto.builder()
                .id(id)
                .name(name)
                .uniqueIdent(uniqueIdent)
                .dashboardLink(dashboardLink)
                .bi(bi.values().stream().map(BiAcc::toDto).collect(Collectors.toList()))
                .build();
    }
}

