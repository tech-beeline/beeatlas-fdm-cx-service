package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.bi.BI;

import java.util.List;

@Repository
public interface BusinessInteractionRepository extends
        JpaRepository<BI, Long>,
        JpaSpecificationExecutor<BI> {

    @Query(value = "SELECT COUNT(*) FROM cj " +
            "JOIN cjsteps ON cj.id = cjsteps.id_cj " +
            "JOIN biincjstep ON cjsteps.id = biincjstep.id_cj_step " +
            "WHERE biincjstep.id_bi = :id AND cj.b_draft = false", nativeQuery = true)
    Long countByBiIdAndDraftFalse(@Param("id") Long id);

    @Query(value = "SELECT DISTINCT * FROM buisnessiteraction " +
            "JOIN biincjstep ON buisnessiteraction.id = biincjstep.id_bi " +
            "WHERE biincjstep.id_bi in :ids and biincjstep.id_cj_step = :cjStepId " +
            "ORDER BY biincjstep.order", nativeQuery = true)
    List<BI> findAllByIdIn(Long cjStepId, List<Long> ids);
}