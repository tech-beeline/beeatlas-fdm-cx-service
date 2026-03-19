/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.bi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import ru.beeline.cxbackend.domain.cj.CJ;

import javax.persistence.*;

@Builder
@Getter
@Setter
@ToString(exclude = "relation")
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bi_relations")
public class BIRelations {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bi_relations_id_generator")
    @SequenceGenerator(name = "bi_relations_id_generator", sequenceName = "bi_relations_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_bi_source")
    @JsonIgnore
    private BI sourceIteraction;

    @ManyToOne
    @JoinColumn(name = "id_bi_target")
    @JsonIgnore
    private BI targetIteraction;

    @ManyToOne
    @JoinColumn(name = "id_cj")
    @JsonIgnore
    private CJ cj;

    private String name;

    @JsonIgnore
    private String descr;
}
