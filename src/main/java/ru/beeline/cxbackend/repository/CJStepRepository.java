package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query(value = "SELECT COUNT(*) FROM buisnessiteraction " +
            "JOIN biincjstep ON biincjstep.id_bi = buisnessiteraction.id " +
            "JOIN cjsteps ON cjsteps.id = biincjstep.id_cj_step " +
            "WHERE cjsteps.id_cj = :id AND buisnessiteraction.b_draft = true", nativeQuery = true)
    Long countByBiIdAndDraft(@Param("id") Long id);
}