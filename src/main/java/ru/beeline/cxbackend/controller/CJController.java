/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.annotation.ApiErrorCodes;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.dto.*;
import ru.beeline.cxbackend.exception.ConflictException;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.service.CJService;
import ru.beeline.cxbackend.service.CJimportFromBpmnService;

import java.util.List;

import static ru.beeline.cxbackend.controller.RequestContext.getUserPermissions;
import static ru.beeline.cxbackend.controller.RequestContext.getUserProducts;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;

@RestController
@RequestMapping
@Api(value = "CX API", tags = "CJ")
public class CJController {

    @Autowired
    private CJService cjService;

    @Autowired
    private CJimportFromBpmnService cJimportFromBpmnService;

    @CustomHeaders
    @GetMapping("/api/cx/v1/product/cj/{id}")
    @ApiOperation(value = "получение CJ продукта по id", response = List.class)
    public CJFullDto getCJById(@PathVariable Long id) {
        return cjService.getFullDtoById(id);
    }

    @CustomHeaders
    @GetMapping("/api/cx/v1/product/cj")
    @ApiOperation(value = "Получение списка CJ", response = List.class)
    public List<CjResponseDto> getCJ(@RequestParam(required = false) Long idProduct,
                                     @RequestParam(required = false, defaultValue = "ALL") String sample,
                                     @RequestParam(required = false, defaultValue = "") String search) {
        return cjService.getAll(idProduct, sample, search);
    }

    @GetMapping("/api/cx/v2/product/cj")
    @ApiOperation(value = "Получение списка CJ", response = List.class)
    public List<CjResponseDtoV2> getCJv2(@RequestParam(name  = "product-id", required = false) Long idProduct,
                                         @RequestParam(required = false, defaultValue = "ALL") String sample,
                                         @RequestParam(required = false, defaultValue = "") String search) {
        return cjService.getAllv2(idProduct, sample, search);
    }

    @ApiErrorCodes({400, 401, 403, 404, 500})
    @GetMapping("api/cx/v2/product/cj/{id}")
    @ApiOperation(value = "получение CJ продукта по id v2", response = List.class)
    public CJFullDtoV2 getCJByIdV2(@PathVariable Long id) {
        return cjService.getFullDtoByIdV2(id);
    }

    @CustomHeaders
    @PatchMapping("/api/cx/v1/bpmn/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Обновление CJ продукта из bpmn")
    public ResponseEntity<Void> updateCJ(@PathVariable Long id) {
        cJimportFromBpmnService.importFromBpmnUpdate(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CustomHeaders
    @PostMapping("/api/cx/v1/bpmn/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Создание CJ продукта из bpmn")
    public ResponseEntity<Void> createCJ(@PathVariable Long id) {
        cJimportFromBpmnService.importFromBpmnCreate(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @CustomHeaders
    @PostMapping("/api/cx/v1/product/{productId}/cj")
    @ResponseBody
    @ApiOperation(value = "Создание CJ продукта")
    public ResponseEntity<CjResponseDto> createCJ(@PathVariable Long productId, @RequestBody CJTagsDto cj) {
        return ResponseEntity.status(HttpStatus.OK).body(cjService.createNewCJ(cj, productId));
    }

    @CustomHeaders
    @PutMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Изменение CJ продукта")
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
    @ApiOperation(value = "Изменение CJ продукта")
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
    @ApiOperation(value = "Удаление CJ")
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
    @ApiOperation(value = "Cохранения ссылки на дашборд мониторинга CJ")
    public ResponseEntity<Void> savingLinkCJ(@PathVariable Long id,
                                             @RequestBody DashboardLinkDTO dashboardLinkDTO) {
        cjService.savingLink(id, dashboardLinkDTO);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}


