/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import lombok.*;
import ru.beeline.cxbackend.domain.Permission;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PermissionDto {

    private Long id;

    private String name;

    private String descr;

    @Enumerated(value = EnumType.STRING)
    private Permission.PermissionType alias;

    private String group;

    private boolean active = false;

}
