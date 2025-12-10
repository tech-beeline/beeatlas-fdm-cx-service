package ru.beeline.cxbackend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class GetProductsByIdsDTO {
    private Integer id;
    private String name;
    private String alias;
    private String struturizrURL;
}
