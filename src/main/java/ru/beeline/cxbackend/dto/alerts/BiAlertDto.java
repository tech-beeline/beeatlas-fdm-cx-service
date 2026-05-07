package ru.beeline.cxbackend.dto.alerts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiAlertDto {
    private Long id;
    private String uniqueIdent;
    private String name;
    private String descr;
    private List<BiStepAlertDto> biSteps;
}

