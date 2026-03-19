/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.bi;

import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bi_in_cj_step")
public class BIInCJStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bi_in_cj_step_id_generator")
    @SequenceGenerator(name = "bi_in_cj_step_id_generator", sequenceName = "bi_in_cj_step_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_bi")
    private BI buisnessIteraction;

    @Column(name = "id_cj_step")
    private Long cjStepId;

    @Column(name = "\"order\"")
    private Long order;

    @Column(name = "id_bi", insertable = false, updatable = false)
    private Long biId;

}
