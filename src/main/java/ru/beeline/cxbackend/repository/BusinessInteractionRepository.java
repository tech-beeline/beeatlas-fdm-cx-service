/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.bi.BI;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessInteractionRepository extends
        JpaRepository<BI, Long>,
        JpaSpecificationExecutor<BI> {

    @Query(value = "SELECT COUNT(*) FROM cx.cj " +
            "JOIN cx.cj_steps ON cj.id = cx.cj_steps.id_cj " +
            "JOIN cx.bi_in_cj_step ON cx.cj_steps.id = cx.bi_in_cj_step.id_cj_step " +
            "WHERE cx.bi_in_cj_step.id_bi = :id AND cx.cj.b_draft = false", nativeQuery = true)
    Long countByBiIdAndDraftFalse(@Param("id") Long id);

    @Query(value = "SELECT DISTINCT * FROM cx.business_iteraction " +
            "JOIN cx.bi_in_cj_step ON cx.business_iteraction.id = cx.bi_in_cj_step.id_bi " +
            "WHERE cx.bi_in_cj_step.id_bi in :ids and cx.bi_in_cj_step.id_cj_step = :cjStepId " +
            "ORDER BY cx.bi_in_cj_step.order", nativeQuery = true)
    List<BI> findAllByIdIn(Long cjStepId, List<Long> ids);

    Optional<BI> findByIdAndDeletedDateIsNull(Long id);

    BI findByIdBpmnAndDeletedDateIsNull(String id);

    BI findByUniqueIdentAndDeletedDateIsNull(String id);
}