package ru.beeline.cxbackend.service.bpmn;

import org.junit.jupiter.api.Test;
import ru.beeline.cxbackend.exception.BadRequestException;
import ru.beeline.cxbackend.model.SequenceFlow;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BpmnOrderCalculatorTest {

    @Test
    void calculateOrder_linearSequence_startsFromOne() {
        List<SequenceFlow> flows = List.of(
                flow("f1", "start", "a"),
                flow("f2", "a", "b"),
                flow("f3", "b", "end")
        );

        Map<String, BpmnOrderAssignment> orders = BpmnOrderCalculator.calculateOrder(List.of("a", "b"), flows);

        assertThat(orders.get("a").getOrder()).isEqualTo(1);
        assertThat(orders.get("a").getOrderTree()).isNull();
        assertThat(orders.get("b").getOrder()).isEqualTo(2);
        assertThat(orders.get("b").getOrderTree()).isNull();
    }

    @Test
    void calculateOrder_parallelBranches_assignsOrderTree() {
        List<SequenceFlow> flows = List.of(
                flow("f1", "start", "fork"),
                flow("f2", "fork", "taskA"),
                flow("f3", "fork", "taskB"),
                flow("f4", "taskA", "join"),
                flow("f5", "taskB", "join"),
                flow("f6", "join", "end")
        );

        Map<String, BpmnOrderAssignment> orders = BpmnOrderCalculator.calculateOrder(List.of("taskA", "taskB", "join"), flows);

        assertThat(orders.get("taskA").getOrder()).isEqualTo(1);
        assertThat(orders.get("taskA").getOrderTree()).isEqualTo("1.1");
        assertThat(orders.get("taskB").getOrder()).isEqualTo(1);
        assertThat(orders.get("taskB").getOrderTree()).isEqualTo("1.2");
        assertThat(orders.get("join").getOrder()).isEqualTo(2);
        assertThat(orders.get("join").getOrderTree()).isNull();
    }

    @Test
    void calculateOrder_branchOrderDeterminedLexicographicallyByFlowId() {
        List<SequenceFlow> flows = List.of(
                flow("flow-b", "fork", "taskB"),
                flow("flow-a", "fork", "taskA"),
                flow("f-start", "start", "fork")
        );

        Map<String, BpmnOrderAssignment> orders = BpmnOrderCalculator.calculateOrder(List.of("taskA", "taskB"), flows);

        assertThat(orders.get("taskA").getOrderTree()).isEqualTo("1.1");
        assertThat(orders.get("taskB").getOrderTree()).isEqualTo("1.2");
    }

    @Test
    void calculateOrder_multipleIndependentFlows_continuesNumbering() {
        List<SequenceFlow> flows = List.of(
                flow("f1", "start1", "a"),
                flow("f2", "a", "end1"),
                flow("f3", "start2", "b"),
                flow("f4", "b", "end2")
        );

        Map<String, BpmnOrderAssignment> orders = BpmnOrderCalculator.calculateOrder(List.of("a", "b"), flows);

        assertThat(orders.get("a").getOrder()).isEqualTo(1);
        assertThat(orders.get("b").getOrder()).isEqualTo(2);
    }

    @Test
    void calculateOrder_unlinkedElements_placedAtEnd() {
        List<SequenceFlow> flows = List.of(
                flow("f1", "start", "a"),
                flow("f2", "a", "end")
        );

        Map<String, BpmnOrderAssignment> orders = BpmnOrderCalculator.calculateOrder(List.of("a", "orphan"), flows);

        assertThat(orders.get("a").getOrder()).isEqualTo(1);
        assertThat(orders.get("orphan").getOrder()).isEqualTo(2);
    }

    @Test
    void calculateOrder_cycleWithoutStart_throwsBadRequest() {
        List<SequenceFlow> flows = List.of(
                flow("f1", "a", "b"),
                flow("f2", "b", "a")
        );

        assertThatThrownBy(() -> BpmnOrderCalculator.calculateOrder(List.of("a", "b"), flows))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Не удалось определить начало цепочки");
    }

    @Test
    void formatOrder_supportsNestedBranchSuffix() {
        assertThat(BpmnOrderCalculator.formatOrder(3, "1")).isEqualTo("3.1");
        assertThat(BpmnOrderCalculator.formatOrder(3, "2")).isEqualTo("3.2");
        assertThat(BpmnOrderCalculator.formatOrder(4, "1.2")).isEqualTo("4.1.2");
    }

    @Test
    void fromFormatted_splitsIntegerOrderAndOrderTree() {
        BpmnOrderAssignment linear = BpmnOrderAssignment.fromFormatted("2");
        assertThat(linear.getOrder()).isEqualTo(2);
        assertThat(linear.getOrderTree()).isNull();

        BpmnOrderAssignment branch = BpmnOrderAssignment.fromFormatted("3.1");
        assertThat(branch.getOrder()).isEqualTo(3);
        assertThat(branch.getOrderTree()).isEqualTo("3.1");

        BpmnOrderAssignment nested = BpmnOrderAssignment.fromFormatted("4.1.2");
        assertThat(nested.getOrder()).isEqualTo(4);
        assertThat(nested.getOrderTree()).isEqualTo("4.1.2");
    }

    private static SequenceFlow flow(String id, String source, String target) {
        return new SequenceFlow(id, source, target);
    }
}
