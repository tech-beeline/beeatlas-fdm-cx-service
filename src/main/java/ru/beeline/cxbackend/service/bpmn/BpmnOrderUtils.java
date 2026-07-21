/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service.bpmn;

import java.util.Comparator;

/**
 * Сравнение и сортировка order в формате "1", "2", "3.1", "4.1.2".
 */
public final class BpmnOrderUtils {

    private BpmnOrderUtils() {
    }

    public static Comparator<String> comparator() {
        return BpmnOrderUtils::compare;
    }

    public static int compare(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            int leftValue = i < leftParts.length ? Integer.parseInt(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? Integer.parseInt(rightParts[i]) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    public static boolean isIntegerOrder(String order) {
        return order != null && !order.contains(".");
    }

    public static String incrementInteger(String order) {
        return String.valueOf(parseInteger(order) + 1);
    }

    public static String decrementInteger(String order) {
        return String.valueOf(parseInteger(order) - 1);
    }

    private static int parseInteger(String order) {
        if (!isIntegerOrder(order)) {
            throw new IllegalArgumentException("Операция поддерживается только для целочисленного order: " + order);
        }
        return Integer.parseInt(order);
    }
}
