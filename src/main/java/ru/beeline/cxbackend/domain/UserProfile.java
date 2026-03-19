/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_profile_id_generator")
    @SequenceGenerator(name = "user_profile_id_generator", sequenceName = "user_profile_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "id_ext")
    @JsonProperty("id_ext")
    private String idExt;

    @Column(name = "full_name")
    @JsonProperty("full_name")
    private String fullName;

    private String login;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_login")
    @JsonProperty("last_login")
    private Date lastLogin;

    private String email;

    @JsonIgnore
    @ApiModelProperty(hidden = true)
    @OneToMany(mappedBy = "userProfile")
    List<UserRoles> userRoles;

}
