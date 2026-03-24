/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.bi;

import lombok.*;

import javax.persistence.*;


@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bi_steps")
public class BiStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bi_steps_id_generator")
    @SequenceGenerator(name = "bi_steps_id_generator", sequenceName = "bi_steps_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bi", nullable = false)
    private BI bi;

    @Column(name = "id_bi", insertable = false, updatable = false)
    private Long biId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_step_type", nullable = false)
    private BiStepTypeEnum stepType;

    @Column(name = "id_step_type", insertable = false, updatable = false)
    private Integer stepTypeId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "id_bpmn", length = 50, nullable = false)
    private String bpmnId;

    private Float latency;

    @Column(name = "error_rate")
    private Float errorRate;

    private Float rps;
}
