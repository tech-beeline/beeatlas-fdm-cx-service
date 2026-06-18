/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CollapsedSubProcess {
    public String id;
    public String name;
    public Integer order;
    public String orderTree;
    public List<BIElement> biElements = new ArrayList<>();

    public String sortKey() {
        return orderTree != null ? orderTree : String.valueOf(order);
    }
}
