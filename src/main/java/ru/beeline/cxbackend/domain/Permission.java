/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permission_id_generator")
    @SequenceGenerator(name = "permission_id_generator", sequenceName = "permission_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String descr;

    @Enumerated(value = EnumType.STRING)
    private PermissionType alias;

    @Column(name = "group_name")
    private String group;

    private boolean deleted;

    public Permission(long id){
        this.id = id;
    }


    @JsonIgnore
    @ApiModelProperty(hidden = true)
    @OneToMany(mappedBy = "role")
    List<RolePermissions> roles;

    public enum PermissionType {
        CREATE_ARTIFACT,
        EDIT_ARTIFACT,
        DELETE_ARTIFACT,
        DESIGN_ARTIFACT
    }

    @Override
    public String toString() {
        return "Permission{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", descr='" + descr + '\'' +
                ", alias=" + alias +
                ", group='" + group + '\'' +
                ", deleted=" + deleted +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission that = (Permission) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
