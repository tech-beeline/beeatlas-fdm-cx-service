/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.cj.CJStep;

import java.util.List;

@Repository
public interface CJStepRepository extends JpaRepository<CJStep, Long> {
    List<CJStep> findAllByCjId(Long cjId);

    CJStep findByCjIdAndOrder(Long cjId, Integer order);

    void deleteAllByCjId(Long cjId);

    @Query(value = "SELECT COUNT(*) FROM cx.business_iteraction " +
            "JOIN cx.bi_in_cj_step ON cx.bi_in_cj_step.id_bi = cx.business_iteraction.id " +
            "JOIN cx.cj_steps ON cx.cj_steps.id = cx.bi_in_cj_step.id_cj_step " +
            "WHERE cx.cj_steps.id_cj = :id AND cx.business_iteraction.b_draft = true", nativeQuery = true)
    Long countByBiIdAndDraft(@Param("id") Long id);

    CJStep findFirstByCjIdAndIdBpmn(Long cjId, String id);

    @Modifying
    @Query("""
                DELETE FROM CJStep cs
                WHERE cs.cjId = :cjId
                  AND (cs.idBpmn IS NULL OR cs.idBpmn NOT IN :ids)
            """)
    void deleteByCjIdAndIdBpmnNotIn(@Param("cjId") long cjId,
                                    @Param("ids") List<String> ids);

    void deleteByCjId(long cjId);
}