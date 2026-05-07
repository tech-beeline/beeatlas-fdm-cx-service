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
public class CjAlertsDto {
    private Long id;
    private String name;
    private String uniqueIdent;
    private String dashboardLink;
    private List<BiAlertDto> bi;
}

