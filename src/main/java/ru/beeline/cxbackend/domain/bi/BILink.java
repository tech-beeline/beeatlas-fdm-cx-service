/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.bi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bi_link")
public class BILink {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bi_link_id_generator")
    @SequenceGenerator(name = "bi_link_id_generator", sequenceName = "bi_link_id_seq", allocationSize = 1)
    @JsonIgnore
    private Long id;

    @Column(name = "url")
    private String url;

    private String descr;

    @ManyToOne
    @JoinColumn(name = "id_bi")
    @JsonIgnore
    private BI idBi;

    @ManyToOne
    @JoinColumn(name = "type_id")
    @JsonIgnore
    private LinkEnum type;

}
