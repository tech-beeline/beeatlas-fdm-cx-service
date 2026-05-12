package ru.beeline.cxbackend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.beeline.cxbackend.dto.owners.AffectedCjDto;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CjOwnersReassignRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public List<AffectedCjDto> findAffectedCj(Long currentUserId) {
        Query q = entityManager.createNativeQuery("""
                SELECT
                    cj.id   AS cj_id,
                    cj.name AS cj_name
                FROM cx.cj cj
                WHERE cj.deleted_date IS NULL
                  AND (
                      cj.id_business_owner = :currentUserId
                      OR EXISTS (
                          SELECT 1
                          FROM cx.cj_tech_owners cto
                          WHERE cto.id_cj = cj.id
                            AND cto.id_user_profile = :currentUserId
                      )
                  )
                ORDER BY cj.id
                """);
        q.setParameter("currentUserId", currentUserId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        return rows.stream()
                .map(r -> AffectedCjDto.builder()
                        .cjId(toLong(r[0]))
                        .cjName(Objects.toString(r[1], null))
                        .build())
                .collect(Collectors.toList());
    }

    public int reassignBusinessOwner(Long currentUserId, Long newUserId) {
        Query q = entityManager.createNativeQuery("""
                UPDATE cx.cj
                SET id_business_owner = :newUserId
                WHERE deleted_date IS NULL
                  AND id_business_owner = :currentUserId
                """);
        q.setParameter("currentUserId", currentUserId);
        q.setParameter("newUserId", newUserId);
        return q.executeUpdate();
    }

    public int deleteDuplicateTechOwnersForReassign(Long currentUserId, Long newUserId) {
        Query q = entityManager.createNativeQuery("""
                DELETE FROM cx.cj_tech_owners cto
                USING cx.cj cj
                WHERE cto.id_cj = cj.id
                  AND cj.deleted_date IS NULL
                  AND cto.id_user_profile = :currentUserId
                  AND :newUserId IS NOT NULL
                  AND EXISTS (
                      SELECT 1
                      FROM cx.cj_tech_owners cto2
                      WHERE cto2.id_cj = cto.id_cj
                        AND cto2.id_user_profile = :newUserId
                  )
                """);
        q.setParameter("currentUserId", currentUserId);
        q.setParameter("newUserId", newUserId);
        return q.executeUpdate();
    }

    public int reassignTechOwner(Long currentUserId, Long newUserId) {
        Query q = entityManager.createNativeQuery("""
                UPDATE cx.cj_tech_owners cto
                SET id_user_profile = :newUserId
                FROM cx.cj cj
                WHERE cto.id_cj = cj.id
                  AND cj.deleted_date IS NULL
                  AND cto.id_user_profile = :currentUserId
                  AND (
                      :newUserId IS NULL
                      OR NOT EXISTS (
                          SELECT 1
                          FROM cx.cj_tech_owners cto2
                          WHERE cto2.id_cj = cto.id_cj
                            AND cto2.id_user_profile = :newUserId
                      )
                  )
                """);
        q.setParameter("currentUserId", currentUserId);
        q.setParameter("newUserId", newUserId);
        return q.executeUpdate();
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long l) return l;
        if (v instanceof Integer i) return i.longValue();
        if (v instanceof Number n) return n.longValue();
        return Long.valueOf(v.toString());
    }
}

