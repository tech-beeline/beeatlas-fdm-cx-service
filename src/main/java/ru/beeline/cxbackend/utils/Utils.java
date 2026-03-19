/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.utils;

public class Utils {

    public static String createUniqueIdent(String prefix, Long id) {
        String idString = String.format("%08d", id);
        return prefix + "." + idString.substring(0, 2) + "." + idString.substring(2, 4) + "." + idString.substring(4,
                                                                                                            6) + "." + idString.substring(
                6);
    }
}
