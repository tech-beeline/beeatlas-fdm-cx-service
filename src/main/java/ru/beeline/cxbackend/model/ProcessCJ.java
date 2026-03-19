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
public class ProcessCJ {
    public String id;
    public List<CollapsedSubProcess> collapsedSubProcesses = new ArrayList<>();
    public List<SequenceFlow> sequenceFlows = new ArrayList<>();
}
