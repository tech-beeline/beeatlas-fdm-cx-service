package ru.beeline.cxbackend.dto.owners;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectedCjDto {
    private Long cjId;
    private String cjName;
}

