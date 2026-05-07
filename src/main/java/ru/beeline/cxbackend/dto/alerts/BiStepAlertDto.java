package ru.beeline.cxbackend.dto.alerts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BiStepAlertDto {
    private Integer idStepType;
    private String name;
    private Float latency;
    private Float errorRate;
    private Float rps;
    private String uniqueIdent;
}

