/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.cj.CJTag;

import java.util.Optional;

@Repository
public interface CJTagRepository extends JpaRepository<CJTag, Long> {
    Optional<CJTag> findByName(String name);
}