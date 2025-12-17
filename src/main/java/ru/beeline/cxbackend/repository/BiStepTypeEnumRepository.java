package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.beeline.cxbackend.domain.bi.BiStepTypeEnum;

public interface BiStepTypeEnumRepository extends JpaRepository<BiStepTypeEnum, Integer> {}