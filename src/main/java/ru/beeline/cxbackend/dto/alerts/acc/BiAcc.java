package ru.beeline.cxbackend.dto.alerts.acc;

import ru.beeline.cxbackend.dto.alerts.BiAlertDto;
import ru.beeline.cxbackend.dto.alerts.BiStepAlertDto;

import java.util.ArrayList;
import java.util.List;

public final class BiAcc {
    public final Long id;
    public final String uniqueIdent;
    public final String name;
    public final String descr;
    public final List<BiStepAlertDto> steps = new ArrayList<>();

    public BiAcc(Long id, String uniqueIdent, String name, String descr) {
        this.id = id;
        this.uniqueIdent = uniqueIdent;
        this.name = name;
        this.descr = descr;
    }

    public BiAlertDto toDto() {
        return BiAlertDto.builder()
                .id(id)
                .uniqueIdent(uniqueIdent)
                .name(name)
                .descr(descr)
                .biSteps(steps)
                .build();
    }
}

