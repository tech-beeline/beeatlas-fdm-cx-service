package ru.beeline.cxbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepDtoV2 {

    private List<BIDto> bi = new ArrayList<>();

    private Long id;

    private Long cjId;

    private Integer order;

    private String name;
    private String uni;

    private String description;
}
