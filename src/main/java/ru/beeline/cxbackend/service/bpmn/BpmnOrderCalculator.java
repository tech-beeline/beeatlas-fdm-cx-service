/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.service.bpmn;

import ru.beeline.cxbackend.exception.BadRequestException;
import ru.beeline.cxbackend.model.SequenceFlow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Назначает order и orderTree элементам BPMN на основании bpmn:sequenceFlow внутри родительского элемента.
 */
public final class BpmnOrderCalculator {

    private BpmnOrderCalculator() {
    }

    public static Map<String, BpmnOrderAssignment> calculateOrder(Collection<String> elementIds,
                                                                  List<SequenceFlow> sequenceFlows) {
        if (elementIds == null || elementIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> elementIdSet = new LinkedHashSet<>(elementIds);
        List<SequenceFlow> flows = sequenceFlows != null ? sequenceFlows : List.of();

        Map<String, List<SequenceFlow>> outgoing = buildOutgoing(flows);
        Set<String> sources = new HashSet<>();
        Set<String> targets = new HashSet<>();
        for (SequenceFlow flow : flows) {
            if (flow.sourceRef != null) {
                sources.add(flow.sourceRef);
            }
            if (flow.targetRef != null) {
                targets.add(flow.targetRef);
            }
        }

        List<String> starts = sources.stream()
                .filter(id -> !targets.contains(id))
                .sorted()
                .collect(Collectors.toList());

        if (starts.isEmpty() && !flows.isEmpty()) {
            throw new BadRequestException("Не удалось определить начало цепочки BPMN-потока");
        }

        Set<String> joinNodes = flows.stream()
                .filter(flow -> flow.targetRef != null)
                .collect(Collectors.groupingBy(flow -> flow.targetRef, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        TraversalContext ctx = new TraversalContext(elementIdSet, outgoing, joinNodes);
        int nextIntegerPosition = 1;

        for (String startId : starts) {
            nextIntegerPosition = traverse(startId, nextIntegerPosition, "", ctx);
        }

        for (String elementId : elementIdSet) {
            if (!ctx.orders.containsKey(elementId)) {
                ctx.orders.put(elementId, BpmnOrderAssignment.fromFormatted(String.valueOf(nextIntegerPosition++)));
            }
        }

        return ctx.orders;
    }

    private static Map<String, List<SequenceFlow>> buildOutgoing(List<SequenceFlow> flows) {
        Map<String, List<SequenceFlow>> outgoing = new HashMap<>();
        for (SequenceFlow flow : flows) {
            if (flow.sourceRef == null || flow.targetRef == null) {
                continue;
            }
            outgoing.computeIfAbsent(flow.sourceRef, key -> new ArrayList<>()).add(flow);
        }
        for (List<SequenceFlow> flowList : outgoing.values()) {
            flowList.sort(Comparator.comparing(flow -> flow.id));
        }
        return outgoing;
    }

    private static int traverse(String nodeId, int position, String branchPath, TraversalContext ctx) {
        boolean continueWithEmptyBranch = false;

        if (ctx.elementIds.contains(nodeId)) {
            if (ctx.visitedElements.contains(nodeId)) {
                return position;
            }
            ctx.visitedElements.add(nodeId);
            boolean isJoin = ctx.joinNodes.contains(nodeId);
            String effectiveBranchPath = isJoin ? "" : branchPath;
            ctx.orders.put(nodeId, BpmnOrderAssignment.fromFormatted(formatOrder(position, effectiveBranchPath)));
            position++;
            continueWithEmptyBranch = isJoin;
        }

        List<SequenceFlow> outFlows = ctx.outgoing.getOrDefault(nodeId, Collections.emptyList());
        if (outFlows.isEmpty()) {
            return position;
        }

        String nextBranchPath = continueWithEmptyBranch ? "" : branchPath;

        if (outFlows.size() == 1) {
            return traverse(outFlows.get(0).targetRef, position, nextBranchPath, ctx);
        }

        int maxPosition = position;
        for (int i = 0; i < outFlows.size(); i++) {
            String newBranchPath = nextBranchPath.isEmpty()
                    ? String.valueOf(i + 1)
                    : nextBranchPath + "." + (i + 1);
            int branchEndPosition = traverse(outFlows.get(i).targetRef, position, newBranchPath, ctx);
            maxPosition = Math.max(maxPosition, branchEndPosition);
        }
        return maxPosition;
    }

    static String formatOrder(int position, String branchPath) {
        if (branchPath == null || branchPath.isEmpty()) {
            return String.valueOf(position);
        }
        return position + "." + branchPath;
    }

    private static final class TraversalContext {
        private final Set<String> elementIds;
        private final Map<String, List<SequenceFlow>> outgoing;
        private final Set<String> joinNodes;
        private final Map<String, BpmnOrderAssignment> orders = new LinkedHashMap<>();
        private final Set<String> visitedElements = new HashSet<>();

        private TraversalContext(Set<String> elementIds,
                                 Map<String, List<SequenceFlow>> outgoing,
                                 Set<String> joinNodes) {
            this.elementIds = elementIds;
            this.outgoing = outgoing;
            this.joinNodes = joinNodes;
        }
    }
}
