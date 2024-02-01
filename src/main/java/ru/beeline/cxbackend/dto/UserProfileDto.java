package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import ru.beeline.cxbackend.domain.UserProfile;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
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

    public UserProfileDto(String idExt, String fullName, String login, String email) {
        this.idExt = idExt;
        this.fullName = fullName;
        this.login = login;
        this.email = email;
    }
}
