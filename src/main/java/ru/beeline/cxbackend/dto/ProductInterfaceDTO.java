/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ProductInterfaceDTO {

    private Integer id;
    private String name;
    private String version;
    private String description;
    private String code;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private List<MapicInterfaceDTO> mapicInterfaces;
    private List<OperationDTO> operations;
}