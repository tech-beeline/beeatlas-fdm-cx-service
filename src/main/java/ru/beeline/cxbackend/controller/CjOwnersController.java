package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.dto.owners.CjOwnersReassignRequestDto;
import ru.beeline.cxbackend.service.CjOwnersReassignService;

@RestController
@Api(value = "CX API", tags = "CJ Owners")
@RequiredArgsConstructor
public class CjOwnersController {
    private final CjOwnersReassignService reassignService;

    @CustomHeaders
    @PatchMapping(value = "/api/cx/v1/cj/owners/reassign", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Массовая замена ответственного пользователя во всех CJ")
    public ResponseEntity<?> reassignOwners(@RequestBody(required = false) CjOwnersReassignRequestDto body) {
        return ResponseEntity.ok(reassignService.reassignOwners(body));
    }
}

