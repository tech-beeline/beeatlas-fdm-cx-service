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
@Table(name = "bi_steps_relations")
public class BiStepRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bi_steps_relations_generator")
    @SequenceGenerator(name = "bi_steps_relations_generator", sequenceName = "bi_steps_relations_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bi_steps", nullable = false)
    private BiStep biStep;

    @Column(name = "id_bi_steps", insertable = false, updatable = false)
    private Integer biStepId;

    @Column(name = "description")
    private String description;

    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "tc_id")
    private Integer tcId;

    @Column(name = "operation_id")
    private Integer operationId;

    @Column(name = "interface_id")
    private Integer interfaceId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "\"order\"")
    private Integer order;
}