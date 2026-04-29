/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.cj.CJTechOwner;

import java.util.List;

@Repository
public interface CJTechOwnerRepository extends JpaRepository<CJTechOwner, Long> {
    List<CJTechOwner> findAllByCj_Id(Long cjId);

    void deleteAllByCj_Id(Long cjId);
}

