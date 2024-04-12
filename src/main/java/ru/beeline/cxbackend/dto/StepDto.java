package ru.beeline.cxbackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
public class StepDto {
    private Long id;

    private Integer order;

    private String name;

    @JsonProperty("id_cj")
    private Long cjId;

    private List<BIDto> bi = new ArrayList<>();
}
