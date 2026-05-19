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

/**
 * Computes BPMN-based order values for elements connected by sequenceFlows.
 *
 * Rules:
 *  - Sequential elements get integer orders: 1, 2, 3, …
 *  - Elements in parallel branches get decimal orders: N.1, N.2, … where N is the
 *    position of the first branched slot after the fork.
 *  - Nested forks extend the decimal part: 3.1 → 3.11, 3.12, …
 *  - Merge points receive the next integer after the max position reached by any branch.
 *  - Elements with no relevant sequence-flow connections are appended last, in
 *    their original list order, with consecutive integers.
 *  - Multiple independent flows inside one parent are processed sequentially;
 *    the second flow continues numbering from where the first left off.
 *  - Throws BadRequestException (HTTP 400) when flows exist but every element is
 *    a targetRef (cycle with no reachable entry point).
 */
public final class BpmnOrderCalculator {

    private BpmnOrderCalculator() {}

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * @param elementIds ordered list of element ids to rank (original document order)
     * @param allFlows   all sequenceFlow objects from the same BPMN parent element
     * @return map elementId → NUMERIC order; iteration order matches assignment order
     */
    public static Map<String, BigDecimal> computeOrders(
            List<String> elementIds,
            List<SequenceFlow> allFlows) {

        if (elementIds == null || elementIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> idSet = new HashSet<>(elementIds);

        // Keep only flows that connect two elements from our set
        List<SequenceFlow> flows = allFlows.stream()
                .filter(sf -> idSet.contains(sf.getSourceRef()) && idSet.contains(sf.getTargetRef()))
                .collect(Collectors.toList());

        // No relevant flows → sequential integer order (document order preserved)
        if (flows.isEmpty()) {
            Map<String, BigDecimal> result = new LinkedHashMap<>();
            int pos = 1;
            for (String id : elementIds) {
                result.put(id, BigDecimal.valueOf(pos++));
            }
            return result;
        }

        // Build adjacency structures
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
        // Deterministic branch ordering: sort each outgoing list by flow id
        outgoing.values().forEach(list -> list.sort(Comparator.comparing(SequenceFlow::getId)));

        // Start elements: in-degree 0 AND have at least one outgoing flow
        List<String> starts = elementIds.stream()
                .filter(id -> inDegree.get(id) == 0 && !outgoing.get(id).isEmpty())
                .collect(Collectors.toList());

        if (starts.isEmpty()) {
            throw new BadRequestException(
                    "Cannot determine BPMN sequence start: all elements have incoming flows (possible cycle)");
        }

        // MergeState tracks how many branches still need to arrive at a merge point
        Map<String, MergeState> mergeStates = new HashMap<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() > 1) {
                mergeStates.put(e.getKey(), new MergeState(e.getValue()));
            }
        }

        Map<String, BigDecimal> orders = new LinkedHashMap<>();
        int nextPosition = 1;

        // Process each independent flow in sequence
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

        // Append elements that had no relevant flow connections
        for (String id : elementIds) {
            if (!orders.containsKey(id)) {
                orders.put(id, BigDecimal.valueOf(nextPosition++));
            }
        }

        return orders;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

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

            // --- Merge-point handling ---
            MergeState ms = mergeStates.get(id);
            if (ms != null) {
                ms.inDegreeRemaining--;
                ms.maxPos = Math.max(ms.maxPos, pos);

                if (ms.inDegreeRemaining > 0) {
                    // Still waiting for other branches to arrive
                    continue;
                }
                // All branches arrived → assign integer order and reset branch context
                pos = ms.maxPos;
                branch = Collections.emptyList();
            }

            if (orders.containsKey(id)) {
                continue; // already assigned (shouldn't normally happen)
            }

            orders.put(id, buildOrder(pos, branch));

            List<SequenceFlow> nexts = outgoing.getOrDefault(id, Collections.emptyList());

            if (nexts.size() == 1) {
                queue.add(new QueueEntry(nexts.get(0).getTargetRef(), pos + 1, branch));
            } else if (nexts.size() > 1) {
                // Fork: each successor gets the same next position but a different branch suffix
                for (int i = 0; i < nexts.size(); i++) {
                    List<Integer> newBranch = new ArrayList<>(branch);
                    newBranch.add(i + 1);
                    queue.add(new QueueEntry(nexts.get(i).getTargetRef(), pos + 1, newBranch));
                }
            }
        }
    }

    /**
     * Converts (position, branchPath) to a BigDecimal order value.
     * Examples:
     *   (3, [])    → 3
     *   (3, [1])   → 3.1
     *   (3, [1,2]) → 3.12   (nested fork: outer branch 1, inner branch 2)
     */
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

    // -----------------------------------------------------------------------
    // Internal data classes
    // -----------------------------------------------------------------------

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
