package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.cj.CJLink;
import ru.beeline.cxbackend.domain.cj.CJTag;

import java.util.Optional;

@Repository
public interface CJLinkRepository extends JpaRepository<CJLink, Integer> {
}