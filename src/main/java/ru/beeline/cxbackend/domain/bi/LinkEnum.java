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
@Table(name = "link_enum")
public class LinkEnum {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "link_enum_id_generator")
    @SequenceGenerator(name = "link_enum_id_generator", sequenceName = "link_enum_id_seq", allocationSize = 1)
    private Long id;

    private String type;
}
