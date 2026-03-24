/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.cj.CJParametersView;

import java.util.List;


@Repository
public interface CJParametersViewRepository extends JpaRepository<CJParametersView, Long> {
    List<CJParametersView> findByCjId(Long cjId);
}
