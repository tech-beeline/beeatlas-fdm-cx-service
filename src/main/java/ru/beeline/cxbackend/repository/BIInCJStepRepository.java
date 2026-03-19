/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.bi.BIInCJStep;

import java.util.List;

@Repository
public interface BIInCJStepRepository extends JpaRepository<BIInCJStep, Long> {
    void deleteAllByCjStepId(Long id);

    void deleteAllByCjStepIdIn(List<Long> cjStepIds);

    void deleteAllByBiId(Long id);

    List<BIInCJStep> findAllByCjStepId(Long cjStepId);

    List<BIInCJStep> findBIInCJStepsByBiId(Long biId);

    BIInCJStep findByCjStepIdAndBiId(Long cjStepId, Long biId);

    @Query(value = "SELECT COUNT(*) FROM cx.cj " +
            "JOIN cx.cj_steps ON cx.cj.id = cx.cj_steps.id_cj " +
            "WHERE cx.cj_steps.id = :id AND cx.cj.b_draft = false", nativeQuery = true)
    Long countByCjStepIdAndSJisDraftFalse(@Param("id") Long id);

}