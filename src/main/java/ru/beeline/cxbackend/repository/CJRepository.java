/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.repository.projection.CjAlertsFlatRow;

import java.util.List;
import java.util.Optional;

@Repository
public interface CJRepository extends JpaRepository<CJ, Long> {

    List<CJ> findAllByDeletedDateIsNull();

    @Query(value = """
            SELECT
                cj.id              AS cj_id,
                cj.name            AS cj_name,
                cj.unique_ident    AS cj_unique_ident,
                cj.dashboard_link  AS cj_dashboard_link,

                bi.id              AS bi_id,
                bi.unique_ident    AS bi_unique_ident,
                bi.name            AS bi_name,
                bi.descr           AS bi_descr,

                bs.id_step_type    AS bs_id_step_type,
                bs.unique_ident    AS bs_unique_ident,
                bs.name            AS bs_name,
                bs.latency         AS bs_latency,
                bs.rps             AS bs_rps,
                bs.error_rate      AS bs_error_rate,
                bs.id              AS bs_id
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