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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.annotation.ApiStandardErrors;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.dto.*;
import ru.beeline.cxbackend.exception.ConflictException;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.dto.alerts.CjAlertsDto;
import ru.beeline.cxbackend.service.CjAlertsService;
import ru.beeline.cxbackend.service.CJService;
import ru.beeline.cxbackend.service.CJimportFromBpmnService;

import java.util.List;
import java.util.Objects;

import static ru.beeline.cxbackend.controller.RequestContext.getUserPermissions;
import static ru.beeline.cxbackend.controller.RequestContext.getUserProducts;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;

@RestController
@RequestMapping
@Tag(
        name = "CJ",
        description = "Customer Journey (CJ): список/получение/создание/изменение/удаление CJ, импорт/обновление из BPMN, алерты."
)
@Slf4j
public class CJController {

    @Autowired
    private CJService cjService;

    @Autowired
    private CJimportFromBpmnService cJimportFromBpmnService;

    @Autowired
    private CjAlertsService cjAlertsService;

    @GetMapping("/api/cx/v1/cj/alerts")
    @ApiStandardErrors
    @Operation(
            summary = "CJ алерты",
            description = "Возвращает список CJ с BI и шагами BI для сценариев алертинга/мониторинга. Заголовки не требуются (исключение в HeaderInterceptor)."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список CJ для алертов",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CjAlertsDto.class)))
    )
    public ResponseEntity<List<CjAlertsDto>> getCjAlerts() {
        List<CjAlertsDto> alerts = Objects.requireNonNullElseGet(cjAlertsService.getCjAlerts(), List::of);
        log.debug("Built cj alerts response, cjCount={}", alerts.size());
        return ResponseEntity.ok(alerts);
    }

    @CustomHeaders
    @GetMapping("/api/cx/v1/product/cj/{id}")
    @ApiStandardErrors
    @Operation(summary = "Получение CJ по id", description = "Возвращает полную CJ (v1) по идентификатору.")
    @ApiResponse(
            responseCode = "200",
            description = "CJ",
            content = @Content(schema = @Schema(implementation = CJFullDto.class))
    )
    public CJFullDto getCJById(@PathVariable Long id) {
        return cjService.getFullDtoById(id);
    }

    @CustomHeaders
    @GetMapping("/api/cx/v1/product/cj")
    @ApiStandardErrors
    @Operation(
            summary = "Список CJ (v1)",
            description = "Возвращает список CJ с фильтрацией по продукту, выборке (`sample`) и поиску по имени. Учитывает доступные пользователю продукты."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список CJ",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CjResponseDto.class)))
    )
    public List<CjResponseDto> getCJ(@RequestParam(required = false) Long idProduct,
                                     @RequestParam(required = false, defaultValue = "ALL") String sample,
                                     @RequestParam(required = false, defaultValue = "") String search) {
        return cjService.getAll(idProduct, sample, search);
    }

    @GetMapping("/api/cx/v2/product/cj")
    @ApiStandardErrors
    @Operation(
            summary = "Список CJ (v2)",
            description = "Версия v2 списка CJ. По `HeaderInterceptor` URI содержит `/v2/product/cj`, поэтому заголовки могут не требоваться."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список CJ (v2)",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CjResponseDtoV2.class)))
    )
    public List<CjResponseDtoV2> getCJv2(@RequestParam(name  = "product-id", required = false) Long idProduct,
                                         @RequestParam(required = false, defaultValue = "ALL") String sample,
                                         @RequestParam(required = false, defaultValue = "") String search) {
        return cjService.getAllv2(idProduct, sample, search);
    }

    @ApiStandardErrors
    @GetMapping("/api/cx/v2/product/cj/{id}")
    @Operation(summary = "Получение CJ по id (v2)", description = "Возвращает полную CJ (v2) по идентификатору.")
    @ApiResponse(
            responseCode = "200",
            description = "CJ (v2)",
            content = @Content(schema = @Schema(implementation = CJFullDtoV3.class))
    )
    public CJFullDtoV3 getCJByIdV3(@PathVariable Long id) {
        return cjService.getFullDtoByIdV3(id);
    }

    @CustomHeaders
    @PatchMapping("/api/cx/v1/bpmn/cj/{id}")
    @ResponseBody
    @ApiStandardErrors
    @Operation(
            summary = "Обновление CJ из BPMN",
            description = "Обновляет CJ, подтягивая BPMN из document-service и применяя изменения. Требуется доступ к продукту и право редактирования."
    )
    @ApiResponse(responseCode = "200", description = "CJ обновлён из BPMN")
    public ResponseEntity<Void> updateCJ(@PathVariable Long id) {
        cJimportFromBpmnService.importFromBpmnUpdate(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CustomHeaders
    @PostMapping("/api/cx/v1/bpmn/cj/{id}")
    @ResponseBody
    @ApiStandardErrors
    @Operation(
            summary = "Создание CJ из BPMN",
            description = "Создаёт CJ для продукта на основании BPMN (document-service). Требуется доступ к продукту."
    )
    @ApiResponse(responseCode = "200", description = "CJ создан из BPMN")
    public ResponseEntity<Void> createCJ(@PathVariable Long id) {
        cJimportFromBpmnService.importFromBpmnCreate(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CustomHeaders
    @PostMapping("/api/cx/v1/product/{productId}/cj")
    @ResponseBody
    @ApiStandardErrors
    @Operation(
            summary = "Создание CJ",
            description = "Создаёт CJ для указанного продукта. Автор берётся из `user-id`. Требуется право `CREATE_ARTIFACT` и доступ к продукту."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Созданный CJ",
            content = @Content(schema = @Schema(implementation = CjResponseDto.class))
    )
    public ResponseEntity<CjResponseDto> createCJ(@PathVariable Long productId, @RequestBody CJTagsDto cj) {
        return ResponseEntity.status(HttpStatus.OK).body(cjService.createNewCJ(cj, productId));
    }

    @CustomHeaders
    @PutMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiStandardErrors
    @Operation(
            summary = "Полная замена CJ (PUT)",
            description = "Заменяет атрибуты CJ. Если CJ опубликован — вернёт 409. Требуется право `EDIT_ARTIFACT` и доступ к продукту."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Обновлённый CJ",
            content = @Content(schema = @Schema(implementation = CjResponseDto.class))
    )
    public ResponseEntity<CjResponseDto> editCJById(@PathVariable Long id, @RequestBody(required = false) CJTagsDto cjDto) {
        if (cjDto == null) {
            return ResponseEntity.ok().build();
        }
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            String message = "CJ с id = " + id + " не найден";
            throw new NotFoundException(message);
        }
        validateAccessProduct(getUserPermissions(), getUserProducts(), currentCJ.getIdProductExt());
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            if (currentCJ.isBDraft() || cjDto.getBDraft()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                        .body(cjService.replaceCJ(currentCJ, cjDto));
            } else {
                throw new ConflictException("CJ с id = " + id + " находится в статусе Опубликован. Редактирование невозможно.");
            }
        } else {
            throw new ForbiddenException("Недостаточно прав для редактирования CJ");
        }
    }

    @CustomHeaders
    @PatchMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiStandardErrors
    @Operation(
            summary = "Частичное обновление CJ (PATCH)",
            description = "Частично обновляет атрибуты CJ. Если CJ опубликован — вернёт 409. Требуется право `EDIT_ARTIFACT` и доступ к продукту."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Обновлённый CJ",
            content = @Content(schema = @Schema(implementation = CjResponseDto.class))
    )
    public ResponseEntity<CjResponseDto> updateCJById(@PathVariable Long id, @RequestBody CJTagsDto cjDto) {
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            throw new NotFoundException("CJ с id = " + id + " не найден");
        }
        validateAccessProduct(getUserPermissions(), getUserProducts(), currentCJ.getIdProductExt());
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            if (currentCJ.isBDraft() || cjDto.getBDraft()) {
                return ResponseEntity.ok(cjService.patchCJ(currentCJ, cjDto));
            } else {
                throw new ConflictException("CJ с id = " + id + " находится в статусе Опубликован. Редактирование невозможно.");
            }
        } else {
            throw new ForbiddenException("Недостаточно прав для редактирования CJ");
        }
    }

    @CustomHeaders
    @DeleteMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiStandardErrors
    @Operation(
            summary = "Удаление CJ",
            description = "Удаляет CJ. Требуется право `DELETE_ARTIFACT`, доступ к продукту и CJ должен быть в черновике."
    )
    @ApiResponse(responseCode = "200", description = "CJ удалён")
    public ResponseEntity<Void> deleteCJById(@PathVariable Long id) {
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            throw new NotFoundException("CJ с id = " + id + " не найден");
        }
        validateAccessProduct(getUserPermissions(), getUserProducts(), currentCJ.getIdProductExt());
        if ((getUserPermissions()).contains(Permission.PermissionType.DELETE_ARTIFACT.toString())) {
            if (currentCJ.isBDraft()) {
                cjService.deleteCJbyId(currentCJ);
                return ResponseEntity.status(HttpStatus.OK).build();
            } else {
                throw new ConflictException("CJ с id = " + id + " находится в статусе Опубликован. Удаление невозможно.");
            }
        } else {
            throw new ForbiddenException("Недостаточно прав для удаления CJ");
        }
    }

    @PatchMapping("/api/v1/cj/{id}")
    @ResponseBody
    @ApiStandardErrors
    @Operation(
            summary = "Сохранение ссылки на дашборд CJ",
            description = "Сохраняет/обновляет ссылку на дашборд мониторинга CJ. Заголовки не требуются (исключение в HeaderInterceptor: `/api/v1/cj/`)."
    )
    @ApiResponse(responseCode = "200", description = "Ссылка сохранена")
    public ResponseEntity<Void> savingLinkCJ(@PathVariable Long id,
                                             @RequestBody DashboardLinkDTO dashboardLinkDTO) {
        cjService.savingLink(id, dashboardLinkDTO);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}


