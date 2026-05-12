/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.repository.projection.CjAlertsFlatRow;

import java.util.List;
import java.util.Optional;

@Repository
public interface CJRepository extends JpaRepository<CJ, Long> {

    List<CJ> findAllByDeletedDateIsNull();

    @Query(value = """
            SELECT
                cj.id              AS "cjId",
                cj.name            AS "cjName",
                cj.unique_ident    AS "cjUniqueIdent",
                cj.dashboard_link  AS "cjDashboardLink",
            
                bi.id              AS "biId",
                bi.unique_ident    AS "biUniqueIdent",
                bi.name            AS "biName",
                bi.descr           AS "biDescr",
            
                bs.id_step_type    AS "bsIdStepType",
                bs.unique_ident    AS "bsUniqueIdent",
                bs.name            AS "bsName",
                bs.latency         AS "bsLatency",
                bs.rps             AS "bsRps",
                bs.error_rate      AS "bsErrorRate",
                bs.id              AS "bsId"
            FROM cx.cj cj
            LEFT JOIN cx.cj_steps cs
                   ON cs.id_cj = cj.id
            LEFT JOIN cx.bi_in_cj_step bics
                   ON bics.id_cj_step = cs.id
            LEFT JOIN cx.business_iteraction bi
                   ON bi.id = bics.id_bi
                  AND bi.deleted_date IS NULL
            LEFT JOIN cx.bi_steps bs
                   ON bs.id_bi = bi.id
            WHERE cj.deleted_date IS NULL
            ORDER BY cj.id, bics."order", bi.id, bs.id
            """, nativeQuery = true)
    List<CjAlertsFlatRow> findCjAlertsFlat();

    List<CJ> findAllByNameContainsIgnoreCaseAndIdProductExtIn(String search, List<Long> idProducts);

    List<CJ> findAllByNameContainsIgnoreCaseAndIdProductExtIsNull(String search);

    List<CJ> findAllByNameContainsIgnoreCase(String search);

    List<CJ> findAllByNameContainsIgnoreCaseAndIdProductExtNotIn(String search, List<Long> idProducts);

    List<CJ> findAllByIdIn(List<Long> ids);

    Optional<CJ> findByIdAndDeletedDateIsNull(Long id);

    CJ findByName(String name);
}