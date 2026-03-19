/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.BIParticipants;

@Repository
public interface BIParticipantsRepository extends JpaRepository<BIParticipants, Long> {
    void deleteAllByBuisnessIteraction(BI bi);
}
