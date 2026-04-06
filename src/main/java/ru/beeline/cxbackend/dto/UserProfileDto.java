/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import ru.beeline.cxbackend.domain.UserProfile;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {

    private Long id;

    @JsonProperty("id_ext")
    private String idExt;

    @JsonProperty("full_name")
    private String fullName;

    private String login;

    @JsonProperty("last_login")
    private Date lastLogin;

    private String email;

    private List<RoleDto> roles;

}
