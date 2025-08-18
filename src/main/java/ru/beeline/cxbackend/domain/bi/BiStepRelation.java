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
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_bi_steps", nullable = false)
    private BiStep biStep;

    @Column(name = "id_bi_steps", insertable = false, updatable = false)
    private Integer biStepId;

    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "\"order\"")
    private Integer order;
}