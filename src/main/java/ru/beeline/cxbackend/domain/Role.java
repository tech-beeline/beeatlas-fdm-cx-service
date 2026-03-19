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

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_id_generator")
    @SequenceGenerator(name = "role_id_generator", sequenceName = "role_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String descr;

    @Enumerated(value = EnumType.STRING)
    private RoleType alias;

    private boolean deleted;

    @Column(name = "is_default")
    private boolean isDefault;

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @ApiModelProperty(hidden = true)
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL)
    List<RolePermissions> permissions;

    @JsonIgnore
    @ApiModelProperty(hidden = true)
    @OneToMany(mappedBy = "role")
    List<UserRoles> userRoles;

    public enum RoleType {
        DEFAULT("Сотрудник"),
        ADMINISTRATOR("Администратор"),
        PRODUCT_OWNER("Владелец продукта"),
        PRODUCT_TEAM_ANALYST_ARCHITECT("Аналитик/Архитектор"),
        PRODUCT_TEAM_MEMBER("Член команды"),
        ENTERPRISE_ARCHITECT("Корп архитектор"),
        IT_MANAGER("ИТ-менеджер"),
        DOMAIN_OWNER("Владелец домена");

        private String roleName;

        public String getRoleName() {
            return roleName;
        }

        private static final RoleType[] values = RoleType.values();

        RoleType(String roleName) {
            this.roleName = roleName;
        }

        public static String getNameById(int id){
            return values[id].getRoleName();
        }
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", descr='" + descr + '\'' +
                ", alias=" + alias +
                ", deleted=" + deleted +
                ", permissions=" + permissions +
                '}';
    }
}
