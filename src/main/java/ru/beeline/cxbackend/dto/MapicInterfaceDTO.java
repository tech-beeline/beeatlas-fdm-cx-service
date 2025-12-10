package ru.beeline.cxbackend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class MapicInterfaceDTO {

    private Integer id;
    private String name;
    private String description;
}