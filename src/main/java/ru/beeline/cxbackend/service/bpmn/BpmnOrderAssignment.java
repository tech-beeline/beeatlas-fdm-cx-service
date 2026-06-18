/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service.bpmn;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Результат расчёта порядка: целый order для линейной нумерации и orderTree для ветвлений BPMN.
 */
@Getter
@AllArgsConstructor
public class BpmnOrderAssignment {

    private final Integer order;
    private final String orderTree;

    public static BpmnOrderAssignment fromFormatted(String formatted) {
        if (formatted == null || formatted.isEmpty()) {
            throw new IllegalArgumentException("formatted order must not be empty");
        }
        if (!formatted.contains(".")) {
            return new BpmnOrderAssignment(Integer.parseInt(formatted), null);
        }
        int integerPart = Integer.parseInt(formatted.substring(0, formatted.indexOf('.')));
        return new BpmnOrderAssignment(integerPart, formatted);
    }

    public String sortKey() {
        return orderTree != null ? orderTree : String.valueOf(order);
    }
}
