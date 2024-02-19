package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.domain.Permission;
import ru.beeline.cxbackend.domain.Product;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.dto.CJDto;
import ru.beeline.cxbackend.dto.CJFullDto;
import ru.beeline.cxbackend.exception.CJNotExistException;
import ru.beeline.cxbackend.service.CJService;
import ru.beeline.cxbackend.service.ProductService;

import java.util.List;

import static ru.beeline.cxbackend.controller.RequestContext.*;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;
import static ru.beeline.cxbackend.utils.Constant.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping
@Api(value = "CX API", tags = "CJ")
public class CJController {

    private Logger logger = LoggerFactory.getLogger(CJController.class);

    @Autowired
    private CJService cjService;

    @Autowired
    private ProductService productService;


    @PostMapping("/api/cx/v1/product/{productId}/cj")
    @ResponseBody
    @ApiOperation(value = "Создание CJ продукта")
    public ResponseEntity createCJ(@PathVariable Long productId, @RequestBody CJDto cj) {
        String errors = "";

        validateAccessProduct(getUserPermissions(),
                getUserProducts(),
                productId);
        if (cjService.findByName(cj.getName()) != null) {
            errors += "Указанное имя CJ уже существует";
            logger.error("422 " + errors);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors);
        }

        if ((getUserPermissions()).contains(Permission.PermissionType.CREATE_ARTIFACT.toString())) {
            if (cj.getName().trim().isEmpty()) {
                errors += "Поле name не может быть пустым.\n";
            }
            if (cj.getUserPortrait().trim().isEmpty()) {
                errors += "Поле user_portrait не может быть пустым.\n";
            }

            if (errors.isEmpty()) {
                Product product = productService.findProductById(productId);
                if (product != null) {
                    CJ newCJ = cjService.createCJ(cj, product, (Long) getHeaders().get(USER_ID_HEADER));
                    logger.info("New cj created: " + newCJ);
                    return ResponseEntity.ok(newCJ);
                } else {
                    errors += "Продукт с id = " + productId + " не найден";
                    logger.error("404 " + errors);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errors);
                }
            } else {
                logger.error("409 " + errors);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
            }
        } else {
            errors += "Недостаточно прав для создания CJ";
            logger.error("403 " + errors);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
        }

    }


    @PutMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Изменение CJ продукта")
    public ResponseEntity editCJById(@PathVariable Long id, @RequestBody CJDto cjDto) throws CJNotExistException {
        String errors = "";
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            String message = "CJ с id = " + id + " не найден";
            logger.error("404 " + message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }
        validateAccessProduct(getUserPermissions(),
                getUserProducts(),
                currentCJ.getIdProductExt());
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            CJ cjByName = cjService.findByName(cjDto.getName());
            if (cjByName != null && !cjByName.getId().equals(currentCJ.getId())) {
                errors += "Указанное имя CJ уже существует";
                logger.error("422 " + errors);
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors);
            }

            if (currentCJ.isBDraft() || cjDto.getBDraft()) {
                return ResponseEntity.ok(cjService.updateCJ(currentCJ, cjDto));
            } else {
                String message = "CJ с id = " + id + " находится в статусе Опубликован. Редактирование невозможно.";
                logger.error("409 " + message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
            }


        } else {
            errors += "Недостаточно прав для редактирования CJ";
            logger.error("403 " + errors);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
        }

    }

    @PatchMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Изменение CJ продукта")
    public ResponseEntity updateCJById(@PathVariable Long id, @RequestBody CJDto cjDto) throws CJNotExistException {
        String errors = "";
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            String message = "CJ с id = " + id + " не найден";
            logger.error("404 " + message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }
        validateAccessProduct(getUserPermissions(),
                getUserProducts(),
                currentCJ.getIdProductExt());
        CJ cjByName = cjService.findByName(cjDto.getName());
        if (cjDto.getName() != null && cjByName != null && !cjByName.getId().equals(id)) {
            errors += "Указанное имя CJ уже существует";
            logger.error("422 " + errors);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors);
        }
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            if (currentCJ.isBDraft() || cjDto.getBDraft()) {
                return ResponseEntity.ok(cjService.updateCJ(currentCJ, cjDto));
            } else {
                String message = "CJ с id = " + id + " находится в статусе Опубликован. Редактирование невозможно.";
                logger.error("409 " + message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
            }
        } else {
            errors += "Недостаточно прав для редактирования CJ";
            logger.error("403 " + errors);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
        }

    }

    @DeleteMapping("/api/cx/v1/product/cj/{id}")
    @ResponseBody
    @ApiOperation(value = "Удаление CJ")
    public ResponseEntity deleteCJById(@PathVariable Long id) throws CJNotExistException {
        String errors = "";
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            String message = "CJ с id = " + id + " не найден";
            logger.error("404 " + message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);

        }
        validateAccessProduct(getUserPermissions(),
                getUserProducts(),
                currentCJ.getIdProductExt());
        if ((getUserPermissions()).contains(Permission.PermissionType.DELETE_ARTIFACT.toString())) {
            if (currentCJ.isBDraft()) {
                cjService.deleteCJbyId(currentCJ);
                return ResponseEntity.status(HttpStatus.OK).build();
            } else {
                String message = "CJ с id = " + id + " находится в статусе Опубликован. Удаление невозможно.";
                logger.error("409 " + message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
            }
        } else {
            errors += "Недостаточно прав для удаления CJ";
            logger.error("403 " + errors);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
        }

    }

    @GetMapping("/api/cx/v1/product/cj/{id}")
    @ApiOperation(value = "получение CJ продукта по id", response = List.class)
    public CJFullDto getCJById(@PathVariable Long id) throws CJNotExistException {
        return cjService.getFullDtoById(id);
    }

    @GetMapping("/api/cx/v1/product/cj")
    @ApiOperation(value = "Получение списка CJ", response = List.class)
    public List<CJ> getCJ(@RequestParam(required = false) String id_product,
                          @RequestParam(required = false, defaultValue = "ALL") String sample,
                          @RequestParam(required = false, defaultValue = "") String search) {
        return cjService.getAll(id_product, sample, search);
    }


}
