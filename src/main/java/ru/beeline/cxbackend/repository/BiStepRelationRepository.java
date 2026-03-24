/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.cxbackend.domain.bi.BiStep;
import ru.beeline.cxbackend.domain.bi.BiStepRelation;

import java.util.List;
import java.util.Set;

public interface BiStepRelationRepository extends JpaRepository<BiStepRelation, Integer> {

    List<BiStepRelation> findByBiStepId(Integer biStepId);

    void deleteByBiStepIdAndIdIn(Integer biStepId, Set<Integer> ids);

    void deleteAllByBiStepIn(List<BiStep> biSteps);
}