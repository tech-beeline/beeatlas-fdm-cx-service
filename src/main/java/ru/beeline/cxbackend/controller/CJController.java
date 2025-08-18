package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.dto.CJDto;
import ru.beeline.cxbackend.dto.CJFullDto;
import ru.beeline.cxbackend.dto.CJFullDtoV2;
import ru.beeline.cxbackend.dto.CJV2Dto;
import ru.beeline.cxbackend.exception.ConflictException;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.exception.UnprocessedEntityException;
import ru.beeline.cxbackend.service.CJService;
import ru.beeline.cxbackend.service.CJimportFromBpmnService;

import java.util.List;

import static ru.beeline.cxbackend.controller.RequestContext.*;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;
import static ru.beeline.cxbackend.utils.Constant.USER_ID_HEADER;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping
@Api(value = "CX API", tags = "CJ")
public class CJController {

    private Logger logger = LoggerFactory.getLogger(CJController.class);

    @Autowired
    private CJService cjService;

    @Autowired
    private CJimportFromBpmnService cJimportFromBpmnService;

    @PostMapping("/api/cx/v1/product/{productId}/cj")
    @ResponseBody
    @ApiOperation(value = "Создание CJ продукта")
    public ResponseEntity createCJ(@PathVariable Long productId, @RequestBody CJDto cj) {
        String errors = "";
        validateAccessProduct(getUserPermissions(), getUserProducts(), productId);
        if (cjService.findByName(cj.getName()) != null) {
            throw new UnprocessedEntityException("Указанное имя CJ уже существует");
        }
        if ((getUserPermissions()).contains(Permission.PermissionType.CREATE_ARTIFACT.toString())) {
            if (cj.getName().trim().isEmpty()) {
                errors += "Поле name не может быть пустым.\n";
            }
            if (cj.getUserPortrait().trim().isEmpty()) {
                errors += "Поле user_portrait не может быть пустым.\n";
            }
            if (errors.isEmpty()) {
                CJ newCJ = cjService.createCJ(cj,
                                              productId,
                                              Long.parseLong(getHeaders().get(USER_ID_HEADER).toString()));
                logger.info("New cj created: " + newCJ);
                return ResponseEntity.status(HttpStatus.OK).body(newCJ);
            } else {
                throw new ConflictException(errors);
            }
        } else {
            throw new ForbiddenException("Недостаточно прав для создания CJ");
        }
    }

    @PostMapping("/api/cx/v1/cj")
    @ResponseBody
    @ApiOperation(value = "Создание CJ продукта")
    public ResponseEntity<CJ> createCJ(@RequestBody CJV2Dto cj) {
        return ResponseEntity.status(HttpStatus.OK).body(cjService.createCJV2(cj));
    }

    @PutMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Изменение CJ продукта")
    public ResponseEntity editCJById(@PathVariable Long id, @RequestBody CJDto cjDto) {
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

    @PatchMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Изменение CJ продукта")
    public ResponseEntity updateCJById(@PathVariable Long id, @RequestBody CJDto cjDto) {
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

    @DeleteMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Удаление CJ")
    public ResponseEntity deleteCJById(@PathVariable Long id) {
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

    @GetMapping("/api/cx/v1/product/cj/{id}")
    @ApiOperation(value = "получение CJ продукта по id", response = List.class)
    public CJFullDto getCJById(@PathVariable Long id) {
        return cjService.getFullDtoById(id);
    }

    @GetMapping("/api/cx/v1/product/cj")
    @ApiOperation(value = "Получение списка CJ", response = List.class)
    public List<CJ> getCJ(@RequestParam(required = false) Long idProduct,
                          @RequestParam(required = false, defaultValue = "ALL") String sample,
                          @RequestParam(required = false, defaultValue = "") String search) {
        return cjService.getAll(idProduct, sample, search);
    }

    @GetMapping("api/cx/v2/product/cj/{id}")
    @ApiOperation(value = "получение CJ продукта по id v2", response = List.class)
    public CJFullDtoV2 getCJByIdV2(@PathVariable Long id) {
        return cjService.getFullDtoByIdV2(id);
    }

    @PostMapping("/api/cx/v1/bpmn/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Создание CJ продукта из bpmn")
    public ResponseEntity createCJ(@PathVariable Long id) {
        cJimportFromBpmnService.importFromBpmn(id);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
