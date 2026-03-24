/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BiStep;
import ru.beeline.cxbackend.domain.bi.BiStepTypeEnum;

import java.util.List;
import java.util.Optional;

public interface BiStepRepository extends JpaRepository<BiStep, Integer> {
    Optional<BiStep> findByBiAndBpmnIdAndStepType(BI bi, String bpmnId, BiStepTypeEnum biStepTypeEnum);

    List<BiStep> findByBi(BI bi);
}