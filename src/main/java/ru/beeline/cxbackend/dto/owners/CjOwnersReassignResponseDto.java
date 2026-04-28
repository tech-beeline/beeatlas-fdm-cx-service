package ru.beeline.cxbackend.dto.owners;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CjOwnersReassignResponseDto {
    private Long currentUserId;
    private Long newUserId;
    private List<AffectedCjDto> affectedCj;
}

