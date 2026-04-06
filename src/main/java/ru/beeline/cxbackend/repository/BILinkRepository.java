/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BILink;
import ru.beeline.cxbackend.domain.bi.LinkEnum;

@Repository
public interface BILinkRepository extends JpaRepository<BILink, Long> {
    void deleteAllByIdBiAndType(BI idBi, LinkEnum type);
}