package ru.beeline.cxbackend.utils;

import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.exception.UnauthorizedException;

import java.util.List;

import static ru.beeline.cxbackend.domain.Permission.PermissionType.DESIGN_ARTIFACT;

public class AccessToProduct {

    public static void validateAccessProduct(List<String> permissions, List<Long> product, Long productId) {
        if (!product.contains(productId) && !permissions.contains(Permission.PermissionType.DESIGN_ARTIFACT.toString()))
            throw new UnauthorizedException("FORBIDDEN");
    }

    public static void validateAccessProduct(List<String> permissions, List<Long> product, BI bi) {
        if (bi.isDraft() && !product.contains(bi.getProductId()) && !permissions.contains(DESIGN_ARTIFACT.toString()))
            throw new UnauthorizedException("FORBIDDEN");
    }
    public static void validateAccessProduct(List<String> permissions, List<Long> product, CJ cj) {
        if (cj.isBDraft() && !product.contains(cj.getIdProductExt()) && !permissions.contains(DESIGN_ARTEFACT.toString()))
            throw new UnauthorizedException("FORBIDDEN");
    }
}
