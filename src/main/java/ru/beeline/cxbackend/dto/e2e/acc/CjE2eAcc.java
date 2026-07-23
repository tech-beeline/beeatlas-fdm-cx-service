/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto.e2e.acc;

import ru.beeline.cxbackend.dto.e2e.CjE2eDto;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CjE2eAcc {
    public final Long id;
    public final String uid;
    public final String name;
    public final Map<Long, BiE2eAcc> bi = new LinkedHashMap<>();

    public CjE2eAcc(Long id, String uid, String name) {
        this.id = id;
        this.uid = uid;
        this.name = name;
    }

    public CjE2eDto toDto() {
        return CjE2eDto.builder()
                .id(id)
                .uid(uid)
                .name(name)
                .bi(bi.values().stream().map(BiE2eAcc::toDto).collect(Collectors.toList()))
                .build();
    }
}
