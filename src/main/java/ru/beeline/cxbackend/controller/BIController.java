/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.annotation.ApiErrorCodes;
import ru.beeline.cxbackend.annotation.ApiStandardErrors;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.dto.BIDto;
import ru.beeline.cxbackend.dto.BIEditabilityDto;
import ru.beeline.cxbackend.dto.BIPostDto;
import ru.beeline.cxbackend.dto.BIV2Dto;
import ru.beeline.cxbackend.dto.PatchRelationStepDto;
import ru.beeline.cxbackend.dto.PatchStepDto;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.service.BusinessInteractionService;

import java.util.List;

import static ru.beeline.cxbackend.controller.RequestContext.getUserPermissions;
import static ru.beeline.cxbackend.utils.Constant.USER_ID_HEADER;

@RestController
@RequestMapping(value = "/api/cx")
@Tag(
        name = "BI Library",
        description = "Библиотека Business Interaction (BI): получение/поиск/создание/редактирование/удаление BI, а также редактирование шагов BI и их связей с технической реализацией (продукты/TC/интерфейсы/операции)."
)
public class BIController {

    @Autowired
    private BusinessInteractionService businessInteractionService;

    @GetMapping("/v1/library/business-interactions")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "Получение BI по продукту",
            description = "Возвращает бизнес-сценарии (BI). Если `id_product` не задан — возвращаются BI без фильтра по продукту."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список BI",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BIDto.class)))
    )
    public ResponseEntity<List<BIDto>> getBI(@RequestParam(value = "id_product", required = false) Long idProduct) {
        return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.getBI(idProduct));
    }

    @GetMapping("/v1/library/business-interactions/find")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "Поиск BI по фильтру",
            description = "Фильтрация по тексту, продукту, статусу и признаку черновика. Параметры можно комбинировать."
    )
    public ResponseEntity<List<BIDto>> getBIByFilter(@RequestParam(required = false) String text,
                                                     @RequestParam(value = "id_product", required = false) Long idProduct,
                                                     @RequestParam(value = "id_status", required = false) Long idStatus,
                                                     @RequestParam(value = "draft", required = false) Boolean isDraft) {

        return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.getProductBIByFilter(text, idProduct, idStatus, isDraft));

    }

    @GetMapping("/v1/library/business-interactions/{id}")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Получение BI по id", description = "Возвращает BI (v1 DTO) по идентификатору.")
    public ResponseEntity<BIDto> getBIById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.getBIById(id));
    }

    @GetMapping("/v2/library/business-interactions/{id}")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Получение BI по id (v2)", description = "Возвращает расширенный BI (v2 DTO) по идентификатору.")
    public ResponseEntity<BIV2Dto> getBIByIdV2(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.getBIByIdV2(id));
    }

    @GetMapping("/v1/library/business-interactions/editability/{id}")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "Проверка редактируемости BI",
            description = "Возвращает признак возможности редактирования BI для текущего пользователя/контекста."
    )
    public ResponseEntity<BIEditabilityDto> getEditabilityDtoBI(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.getEditabilityBI(id));
    }

    @PostMapping("/v1/library/business-interactions")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "Создание BI",
            description = "Создаёт BI. Требуется право `CREATE_ARTIFACT` в `user-permission`."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Созданный BI",
            content = @Content(schema = @Schema(implementation = BIDto.class))
    )
    public ResponseEntity<BIDto> createBI(@RequestBody BIPostDto bi) {
        if ((getUserPermissions()).contains(Permission.PermissionType.CREATE_ARTIFACT.toString())) {
            return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.createBI(bi));
        } else {
            throw new ForbiddenException("Недостаточно прав для создания BI");
        }
    }

    @PatchMapping("/v1/library/business-interactions/{id}")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "Редактирование BI",
            description = "Обновляет BI. Требуется право `EDIT_ARTIFACT` в `user-permission`."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Обновлённый BI",
            content = @Content(schema = @Schema(implementation = BIDto.class))
    )
    public ResponseEntity<BIDto> patchBI(@RequestBody BI bi,
                                         @PathVariable Long id
    ) {
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.patchBI(id, bi));
        } else {
            throw new ForbiddenException("Недостаточно прав для редактирования BI");
        }
    }

    @ApiErrorCodes({401, 403, 404, 400, 500})
    @CustomHeaders
    @ApiStandardErrors
    @PatchMapping("/v1/library/business-interactions/step/{id}")
    @Operation(
            summary = "Редактирование шага BI",
            description = "Обновляет атрибуты шага внутри бизнес-сценария (BI) по id шага."
    )
    public ResponseEntity<Void> updateBiStep(@PathVariable Integer id, @RequestBody PatchStepDto patchStepDto) {
        businessInteractionService.patchBiStep(id, patchStepDto);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @ApiErrorCodes({401, 403, 404, 400, 500})
    @ResponseStatus(HttpStatus.CREATED)
    @CustomHeaders
    @ApiStandardErrors
    @PatchMapping("/v1/library/business-interactions/step/{id}/relation")
    @Operation(
            summary = "Обновление связей тех. реализации у шага BI (PATCH)",
            description = "Обновляет связи шага BI с тех. реализацией/возможностями. Используйте PATCH для частичного обновления."
    )
    public ResponseEntity<Void> updateRelationBiStep(@PathVariable Integer id,
                                                     @RequestBody List<PatchRelationStepDto> patchRelationStepDtos,
                                                     @RequestHeader(value = USER_ID_HEADER, required = false) String userId) {
        businessInteractionService.updateRelationBiStep(id, patchRelationStepDtos, userId, true);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @ApiErrorCodes({401, 403, 404, 400, 500})
    @ResponseStatus(HttpStatus.CREATED)
    @CustomHeaders
    @ApiStandardErrors
    @PutMapping("/v1/library/business-interactions/step/{id}/relation")
    @Operation(
            summary = "Замена связей тех. реализации у шага BI (PUT)",
            description = "Полностью заменяет набор связей шага BI с тех. реализацией/возможностями."
    )
    public ResponseEntity<Void> putRelationBiStep(@PathVariable Integer id,
                                                     @RequestBody List<PatchRelationStepDto> patchRelationStepDtos,
                                                     @RequestHeader(value = USER_ID_HEADER, required = false) String userId) {
        businessInteractionService.updateRelationBiStep(id, patchRelationStepDtos, userId, false);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/v1/library/business-interactions/{id}")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "Удаление BI",
            description = "Удаляет BI по id. Требуется право `DELETE_ARTIFACT` в `user-permission`."
    )
    @ApiResponse(responseCode = "200", description = "BI удалён")
    public ResponseEntity<Void> deleteBIById(@PathVariable Long id) {
        if ((getUserPermissions()).contains(Permission.PermissionType.DELETE_ARTIFACT.toString())) {
            businessInteractionService.deleteBIById(id);
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            throw new ForbiddenException("Недостаточно прав для удаления BI");
        }
    }
}