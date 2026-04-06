/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.bi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import ru.beeline.cxbackend.domain.bi.ref.BIParticipant;

import javax.persistence.*;

@Builder
@Getter
@Setter
@ToString(exclude = "buisnessIteraction")
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bi_participants")
public class BIParticipants {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bi_participants_id_generator")
    @SequenceGenerator(name = "bi_participants_id_generator", sequenceName = "bi_participants_id_seq", allocationSize = 1)
    private Long id;

    private String descr;

    @Column(name = "id_type", insertable = false, updatable = false)
    private Long idType;

    @ManyToOne
    @JoinColumn(name = "id_type")
    @JsonIgnore
    private BIParticipant   participantEnum;

    @ManyToOne
    @JoinColumn(name = "id_bi")
    @JsonIgnore
    private BI buisnessIteraction;

    private String value;
}
