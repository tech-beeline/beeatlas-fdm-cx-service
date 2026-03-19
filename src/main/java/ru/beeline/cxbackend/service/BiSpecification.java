/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;

@Service
public class BiSpecification {

    public static Specification<BI> hasProductId(Long productId) {
        return (root, query, cb) -> {
            if (productId == null) {
                return cb.isTrue(cb.literal(true));
            }
            return cb.equal(root.get("productId"), productId);
        };
    }

    public static Specification<BI> hasStatusId(BIStatus statusId) {
        return (root, query, cb) -> {
            if (statusId == null) {
                return cb.isTrue(cb.literal(true));
            }
            return cb.equal(root.get("status"), statusId);
        };
    }

    public static Specification<BI> isDraft(Boolean isDraft) {
        return (root, query, cb) -> {
            if (isDraft == null) {
                return cb.isTrue(cb.literal(true));
            }
            return cb.equal(root.get("isDraft"), isDraft);
        };
    }

    public static Specification<BI> hasNameContaining(String text) {
        return (root, query, cb) -> {
            if (text == null) {
                return cb.isTrue(cb.literal(true));
            }
            return cb.like(cb.lower(root.get("name")), "%" + text.toLowerCase() + "%");
        };
    }

    public static Specification<BI> hasBINumberContaining(String number) {
        return (root, query, cb) -> {
            if (number == null) {
                return cb.isTrue(cb.literal(true));
            }
            return cb.like(cb.lower(root.get("uniqueIdent")), "%" + number.toLowerCase() + "%");
        };
    }

    public static Specification<BI> isDeletedDateNull() {
        return (root, query, cb) -> cb.isNull(root.get("deletedDate"));
    }
}

