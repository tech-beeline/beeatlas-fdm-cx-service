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
public class StepDtoV3 {

    private List<BIDtoV3> bi = new ArrayList<>();

    private Long id;

    private Long cjId;

    private Integer order;

    private String name;

    private String description;
}
