/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.utils;

public class AccessToProduct {

    public static void validateProductId(Long productId) {
        if (productId == null) {
            throw new IllegalArgumentException("Параметр productId не должен быть пустым.");
        }
    }
}
