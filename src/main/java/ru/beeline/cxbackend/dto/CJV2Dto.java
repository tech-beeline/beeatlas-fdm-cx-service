package ru.beeline.cxbackend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CJV2Dto {

    private String name;
    private String userPortrait;
    private Boolean draft;
    private Integer productId;
}
