/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.utils;

import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.exception.ForbiddenException;

import java.util.List;

import static ru.beeline.cxbackend.domain.Permission.PermissionType.DESIGN_ARTIFACT;

public class AccessToProduct {

    public static void validateProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Параметр productId не должен быть пустым.");
        }
    }

    public static void validateAccessProduct(List<String> permissions, List<Long> product, Long productId) {
        if (!product.contains(productId) && !permissions.contains(Permission.PermissionType.DESIGN_ARTIFACT.toString()))
            throw new ForbiddenException("FORBIDDEN");
    }

    public static void validateAccessProduct(List<String> permissions) {
        if (!permissions.contains(Permission.PermissionType.DESIGN_ARTIFACT.toString()))
            throw new ForbiddenException("FORBIDDEN");
    }

    public static void validateAccessProduct(List<String> permissions, List<Long> product, CJ cj) {
        if (cj.isBDraft() && !product.contains(cj.getIdProductExt()) && !permissions.contains(DESIGN_ARTIFACT.toString()))
            throw new ForbiddenException("FORBIDDEN");
    }

    public static void validateAccessProduct(List<String> permissions, List<Long> product, BI bi) {
        if (bi.isDraft() && !product.contains(bi.getProductId()) && !permissions.contains(DESIGN_ARTIFACT.toString()))
            throw new ForbiddenException("FORBIDDEN");
    }

}
