/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BiStep;
import ru.beeline.cxbackend.domain.bi.BiStepTypeEnum;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BiStepRepository extends JpaRepository<BiStep, Integer> {
    Optional<BiStep> findByBiAndBpmnIdAndStepType(BI bi, String bpmnId, BiStepTypeEnum biStepTypeEnum);

    @Query("SELECT bs FROM BiStep bs WHERE bs.bi = :bi")
    List<BiStep> findByBi(@Param("bi") BI bi);

    List<BiStep> findByBiIn(List<BI> bi);

    List<BiStep> findAllByIdIn(Collection<Integer> biStepIds);
}