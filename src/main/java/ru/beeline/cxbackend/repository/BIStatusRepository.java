/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;

@Repository
public interface BIStatusRepository extends JpaRepository<BIStatus, Long> {
}