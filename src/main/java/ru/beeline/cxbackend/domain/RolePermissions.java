/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "role_permissions")
public class RolePermissions {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_permissions_id_rec_generator")
    @SequenceGenerator(name = "role_permissions_id_rec_generator", sequenceName = "role_permissions_id_rec_seq", allocationSize = 1)
    @Column(name = "id_rec")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_permission")
    private Permission permission;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_role")
    private Role role;


}
