/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.cj.CJ;

import java.util.List;
import java.util.Optional;

@Repository
public interface CJRepository extends JpaRepository<CJ, Long> {

    List<CJ> findAllByNameContainsIgnoreCaseAndIdProductExtIn(String search, List<Long> idProducts);
    List<CJ> findAllByNameContainsIgnoreCaseAndIdProductExtIsNull(String search);

    List<CJ> findAllByNameContainsIgnoreCase(String search);

    List<CJ> findAllByNameContainsIgnoreCaseAndIdProductExtNotIn(String search, List<Long> idProducts);

    List<CJ> findAllByIdIn(List<Long> ids);

    Optional<CJ> findByIdAndDeletedDateIsNull(Long id);

    CJ findByName(String name);
}