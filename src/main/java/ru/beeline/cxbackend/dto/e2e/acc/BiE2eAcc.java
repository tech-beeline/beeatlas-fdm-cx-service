/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto.e2e.acc;

import ru.beeline.cxbackend.dto.e2e.BiE2eDto;
import ru.beeline.cxbackend.dto.e2e.BiStepE2eDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BiE2eAcc {
    public final Long id;
    public final String uid;
    public final String name;
    public final Set<String> stepUids = new HashSet<>();
    public final List<BiStepE2eDto> steps = new ArrayList<>();

    public BiE2eAcc(Long id, String uid, String name) {
        this.id = id;
        this.uid = uid;
        this.name = name;
    }

    public BiE2eDto toDto() {
        return BiE2eDto.builder()
                .id(id)
                .uid(uid)
                .name(name)
                .biSteps(new ArrayList<>(steps))
                .build();
    }
}
