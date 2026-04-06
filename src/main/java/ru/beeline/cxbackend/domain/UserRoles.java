/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_roles")
public class UserRoles {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_roles_id_rec_generator")
    @SequenceGenerator(name = "user_roles_id_rec_generator", sequenceName = "user_roles_id_rec_seq", allocationSize = 1)
    @Column(name = "id_rec")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_profile")
    private UserProfile userProfile;

    @ManyToOne
    @JoinColumn(name = "id_role")
    private Role role;
}
