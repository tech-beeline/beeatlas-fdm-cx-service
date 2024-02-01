package ru.beeline.cxbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.domain.cj.CJ;

import java.util.List;

@Repository
public interface CJRepository extends JpaRepository<CJ, Long> {

    List<CJ> findAllByNameContainsIgnoreCaseAndIdProductExtIn(String search, List<String> idProducts);

    List<CJ> findAllByNameContainsIgnoreCase(String search);

    List<CJ> findAllByNameContainsIgnoreCaseAndIdProductExtNotIn(String search, List<String> idProducts);

    @Query(value = "SELECT upe.ID_PRODUCT_EXT FROM user_product_ext upe " +
            "INNER JOIN user_profile up ON up.ID = upe.ID_PROFILE " +
            "WHERE up.LOGIN = ?1",
            nativeQuery = true)
    List<String> findProductIdsByUserLogin(String login);

    List<CJ> findAllByIdIn(List<Long> ids);

    CJ findByName(String name);
}