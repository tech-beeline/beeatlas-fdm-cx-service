/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.utils;

import ru.beeline.cxbackend.exception.BadRequestException;
import ru.beeline.cxbackend.model.SequenceFlow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public final class BpmnOrderCalculator {

    private BpmnOrderCalculator() {}

    public static Map<String, BigDecimal> computeOrders(
            List<String> elementIds,
            List<SequenceFlow> allFlows) {

        if (elementIds == null || elementIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> idSet = new HashSet<>(elementIds);

        List<SequenceFlow> flows = allFlows.stream()
                .filter(sf -> idSet.contains(sf.getSourceRef()) && idSet.contains(sf.getTargetRef()))
                .collect(Collectors.toList());

        if (flows.isEmpty()) {
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            int pos = 1;
            for (String id : elementIds) {
                result.put(id, BigDecimal.valueOf(pos++));
            }
            return result;
        }

        Map<String, List<SequenceFlow>> outgoing = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (String id : elementIds) {
            outgoing.put(id, new ArrayList<>());
            inDegree.put(id, 0);
        }
        for (SequenceFlow sf : flows) {
            outgoing.get(sf.getSourceRef()).add(sf);
            inDegree.merge(sf.getTargetRef(), 1, Integer::sum);
        }
        outgoing.values().forEach(list -> list.sort(Comparator.comparing(SequenceFlow::getId)));

        List<String> starts = elementIds.stream()
                .filter(id -> inDegree.get(id) == 0 && !outgoing.get(id).isEmpty())
                .collect(Collectors.toList());

        if (starts.isEmpty()) {
            throw new BadRequestException(
                    "Cannot determine BPMN sequence start: all elements have incoming flows (possible cycle)");
        }

        Map<String, MergeState> mergeStates = new HashMap<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() > 1) {
                mergeStates.put(e.getKey(), new MergeState(e.getValue()));
            }
        }

        Map<String, BigDecimal> orders = new LinkedHashMap<>();
        int nextPosition = 1;

        for (String start : starts) {
            Queue<QueueEntry> queue = new ArrayDeque<>();
            queue.add(new QueueEntry(start, nextPosition, Collections.emptyList()));
            drainQueue(queue, orders, outgoing, mergeStates);

            int maxFloorPos = orders.values().stream()
                    .map(d -> d.setScale(0, RoundingMode.FLOOR).intValue())
                    .max(Integer::compareTo)
                    .orElse(nextPosition - 1);
            nextPosition = maxFloorPos + 1;
        }

        for (String id : elementIds) {
            if (!orders.containsKey(id)) {
                orders.put(id, BigDecimal.valueOf(nextPosition++));
            }
        }

        return orders;
    }

    private static void drainQueue(
            Queue<QueueEntry> queue,
            Map<String, BigDecimal> orders,
            Map<String, List<SequenceFlow>> outgoing,
            Map<String, MergeState> mergeStates) {

        while (!queue.isEmpty()) {
            QueueEntry entry = queue.poll();
            String id = entry.id;
            int pos = entry.position;
            List<Integer> branch = entry.branchPath;

            MergeState ms = mergeStates.get(id);
            if (ms != null) {
                ms.inDegreeRemaining--;
                ms.maxPos = Math.max(ms.maxPos, pos);

                if (ms.inDegreeRemaining > 0) {
                    continue;
                }
                pos = ms.maxPos;
                branch = Collections.emptyList();
            }

            if (orders.containsKey(id)) {
                continue;
            }

            orders.put(id, buildOrder(pos, branch));

            List<SequenceFlow> nexts = outgoing.getOrDefault(id, Collections.emptyList());

            if (nexts.size() == 1) {
                queue.add(new QueueEntry(nexts.get(0).getTargetRef(), pos + 1, branch));
            } else if (nexts.size() > 1) {
                for (int i = 0; i < nexts.size(); i++) {
                    List<Integer> newBranch = new ArrayList<>(branch);
                    newBranch.add(i + 1);
                    queue.add(new QueueEntry(nexts.get(i).getTargetRef(), pos + 1, newBranch));
                }
            }
        }
    }

    private static BigDecimal buildOrder(int position, List<Integer> branchPath) {
        if (branchPath.isEmpty()) {
            return BigDecimal.valueOf(position);
        }
        StringBuilder sb = new StringBuilder(Integer.toString(position)).append('.');
        for (int b : branchPath) {
            sb.append(b);
        }
        return new BigDecimal(sb.toString());
    }

    private static final class QueueEntry {
        final String id;
        final int position;
        final List<Integer> branchPath;

        QueueEntry(String id, int position, List<Integer> branchPath) {
            this.id = id;
            this.position = position;
            this.branchPath = branchPath;
        }
    }

    private static final class MergeState {
        int inDegreeRemaining;
        int maxPos;

        MergeState(int inDegree) {
            this.inDegreeRemaining = inDegree;
            this.maxPos = 0;
        }
    }
}
