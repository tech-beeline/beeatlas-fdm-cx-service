/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.cj;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cj_steps")
public class CJStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cj_steps_id_generator")
    @SequenceGenerator(name = "cj_steps_id_generator", sequenceName = "cj_steps_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "\"order\"")
    private Integer order;

    private String name;

    private String description;

    @Column(name = "id_cj")
    @JsonProperty("id_cj")
    private Long cjId;

    @Column(name = "id_bpmn", length = 50, nullable = true)
    private String idBpmn;
}
