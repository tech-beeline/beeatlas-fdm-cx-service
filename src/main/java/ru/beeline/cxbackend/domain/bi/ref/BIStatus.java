/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.bi.ref;

import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bi_status")
public class BIStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bi_status_id_generator")
    @SequenceGenerator(name = "bi_status_id_generator", sequenceName = "bi_status_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String descr;

}
