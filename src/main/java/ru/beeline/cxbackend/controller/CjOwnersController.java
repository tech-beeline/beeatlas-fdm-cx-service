package ru.beeline.cxbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.cxbackend.annotation.ApiStandardErrors;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.dto.owners.CjOwnersReassignRequestDto;
import ru.beeline.cxbackend.service.CjOwnersReassignService;

@RestController
@Tag(
        name = "CJ Owners",
        description = "Операции с владельцами CJ: массовая замена бизнес/тех владельцев по всем CJ (администрирование)."
)
@RequiredArgsConstructor
public class CjOwnersController {
    private final CjOwnersReassignService reassignService;

    @CustomHeaders
    @PatchMapping(value = "/api/cx/v1/cj/owners/reassign", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiStandardErrors
    @Operation(
            summary = "Массовая замена владельца в CJ",
            description = "Массово заменяет текущего владельца CJ на нового. Обычно требует роль ADMINISTRATOR в `user-roles`."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Результат операции (список затронутых CJ и входные параметры)",
            content = @Content(schema = @Schema(implementation = Object.class))
    )
    public ResponseEntity<Object> reassignOwners(@RequestBody(required = false) CjOwnersReassignRequestDto body) {
        return ResponseEntity.ok(reassignService.reassignOwners(body));
    }
}

