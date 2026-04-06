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
@Table(name = "bi_participant_enum")
public class BIParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bi_participant_enum_id_generator")
    @SequenceGenerator(name = "bi_participant_enum_id_generator", sequenceName = "bi_participant_enum_id_seq", allocationSize = 1)
    private Long id;

    private String name;

}
