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
import ru.beeline.cxbackend.dto.CJDto;
import ru.beeline.cxbackend.dto.CJFullDto;
import ru.beeline.cxbackend.dto.CJFullDtoV2;
import ru.beeline.cxbackend.dto.CJPostDto;
import ru.beeline.cxbackend.exception.ConflictException;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.exception.UnprocessedEntityException;
import ru.beeline.cxbackend.service.CJService;
import ru.beeline.cxbackend.service.CJimportFromBpmnService;

import java.util.List;

import static ru.beeline.cxbackend.controller.RequestContext.getUserPermissions;
import static ru.beeline.cxbackend.controller.RequestContext.getUserProducts;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;

@CrossOrigin(origins = "*", allowedHeaders = "*")
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
    public List<CJ> getCJ(@RequestParam(required = false) Long idProduct,
                          @RequestParam(required = false, defaultValue = "ALL") String sample,
                          @RequestParam(required = false, defaultValue = "") String search) {
        return cjService.getAll(idProduct, sample, search);
    }

    @ApiErrorCodes({400, 401, 403, 404, 500})
    @CustomHeaders
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
    public ResponseEntity<CJ> createCJ(@PathVariable Long productId, @RequestBody CJPostDto cj) {
        return ResponseEntity.status(HttpStatus.OK).body(cjService.createNewCJ(cj, productId));
    }

    @CustomHeaders
    @PutMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Изменение CJ продукта")
    public ResponseEntity<CJ> editCJById(@PathVariable Long id, @RequestBody CJDto cjDto) {
        String errors = "";
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            String message = "CJ с id = " + id + " не найден";
            throw new NotFoundException(message);
        }
        validateAccessProduct(getUserPermissions(), getUserProducts(), currentCJ.getIdProductExt());
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            CJ cjByName = cjService.findByName(cjDto.getName());
            if (cjByName != null && !cjByName.getId().equals(currentCJ.getId())) {
                throw new UnprocessedEntityException("Указанное имя CJ уже существует");
            }
            if (currentCJ.isBDraft() || cjDto.getBDraft()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                        .body(cjService.updateCJ(currentCJ, cjDto));
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
    public ResponseEntity<CJ> updateCJById(@PathVariable Long id, @RequestBody CJDto cjDto) {
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            throw new NotFoundException("CJ с id = " + id + " не найден");
        }
        validateAccessProduct(getUserPermissions(), getUserProducts(), currentCJ.getIdProductExt());
        CJ cjByName = cjService.findByName(cjDto.getName());
        if (cjDto.getName() != null && cjByName != null && !cjByName.getId().equals(id)) {
            throw new UnprocessedEntityException("Указанное имя CJ уже существует");
        }
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            if (currentCJ.isBDraft() || cjDto.getBDraft()) {
                return ResponseEntity.ok(cjService.updateCJ(currentCJ, cjDto));
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
}

