package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.cxbackend.domain.bi.BiStepRelation;

public interface BiStepRelationRepository extends JpaRepository<BiStepRelation, Integer> {}