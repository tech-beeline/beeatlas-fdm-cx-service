/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain.cj;

import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@ToString(exclude = "cj")
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cj_tech_owners", schema = "cx")
public class CJTechOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cj_tech_owners_id_generator")
    @SequenceGenerator(
            name = "cj_tech_owners_id_generator",
            sequenceName = "cj_tech_owners_id_seq",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cj", nullable = false)
    private CJ cj;

    @Column(name = "id_user_profile")
    private Long idUserProfile;
}

