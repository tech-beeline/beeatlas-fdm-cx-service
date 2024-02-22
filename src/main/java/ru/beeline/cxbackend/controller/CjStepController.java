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
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.CjStepDto;
import ru.beeline.cxbackend.exception.CJNotExistException;
import ru.beeline.cxbackend.exception.StepNotExistException;
import ru.beeline.cxbackend.service.*;

import java.util.List;

import static ru.beeline.cxbackend.controller.RequestContext.getUserPermissions;
import static ru.beeline.cxbackend.controller.RequestContext.getUserProducts;
import static ru.beeline.cxbackend.utils.AccessToProduct.validateAccessProduct;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping
@Api(value = "CX API", tags = "CJ Step")
public class CjStepController {

    @Autowired
    private CJStepService cjStepService;

    @Autowired
    private CJService cjService;

    private Logger logger = LoggerFactory.getLogger(CjStepController.class);

    @GetMapping("/api/cx/v1/product/cj/{id}/step")
    @ResponseBody
    @ApiOperation(value = "Получение коллекции шагов CJ")
    public ResponseEntity getCJSteps(@PathVariable Long id) {
        List<CJStep> steps = cjStepService.getStepByCJId(id);
        return ResponseEntity.ok(steps);
    }

    @PostMapping("/api/cx/v1/product/cj/{id}/step")
    @ResponseBody
    @ApiOperation(value = "Добавление шага в коллекцию шагов CJ")
    public ResponseEntity addCJStep(@PathVariable Long id,
                                    @RequestBody CjStepDto cjStepDto) throws CJNotExistException {
        String errors = "";
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            validateAccessProduct(getUserPermissions(),
                    getUserProducts(),
                    currentCJ.getIdProductExt());
            String message = "CJ с id = " + id + " не найден";
            logger.error("404 " + message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            if (currentCJ.isBDraft()) {
                return ResponseEntity.ok(cjStepService.addStep(id, cjStepDto));
            } else {
                String message = "CJ с id = " + id + " находится в статусе Опубликован. Добавление шага невозможно.";
                logger.error("409 " + message);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(message);
            }
        } else {
            errors += "Недостаточно прав для добавления шага CJ";
            logger.error("403 " + errors);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
        }
    }

    @GetMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @ApiOperation(value = "Получение шага CJ по id")
    public ResponseEntity getCJStepById(@PathVariable Long id) {
        CJStep cjStep = null;
        try {
            cjStep = cjStepService.getStepById(id);
        } catch (StepNotExistException e) {
            String message = "CJ шаг с id = " + id + " не найден";
            logger.error("404 " + message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }
        return ResponseEntity.ok(cjStep);
    }

    @PatchMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @ApiOperation(value = "Изменение шага CJ")
    public ResponseEntity updateCJStep(@PathVariable Long id, @RequestBody CjStepDto cjStepDto) throws CJNotExistException {
        String errors = "";

        CJStep cjStep = null;
        try {
            cjStep = cjStepService.getStepById(id);
        } catch (StepNotExistException e) {
            String message = "CJ шаг с id = " + id + " не найден";
            logger.error("404 " + message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }

        Long productId = cjService.getById(cjStep.getCjId()).getIdProductExt();
        validateAccessProduct(getUserPermissions(),
                getUserProducts(),
                productId);

        if ((getUserPermissions()).contains(Permission.PermissionType.CREATE_ARTIFACT.toString())) {
            return ResponseEntity.ok(cjStepService.updateStep(cjStep, cjStepDto));
        } else {
            errors += "Недостаточно прав для изменения шага CJ";
            logger.error("403 " + errors);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
        }
    }

    @DeleteMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @ApiOperation(value = "Удаление шага CJ")
    public ResponseEntity deleteCJStep(@PathVariable Long id) throws CJNotExistException {

        String errors = "";

        CJStep cjStep = null;
        try {
            cjStep = cjStepService.getStepById(id);
        } catch (StepNotExistException e) {
            String message = "CJ шаг с id = " + id + " не найден";
            logger.error("404 " + message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message);
        }
        Long productId = cjService.getById(cjStep.getCjId()).getIdProductExt();
        validateAccessProduct(getUserPermissions(),
                getUserProducts(), productId);

        if ((getUserPermissions()).contains(Permission.PermissionType.DELETE_ARTIFACT.toString())) {
            cjStepService.deleteStep(cjStep);
            return ResponseEntity.ok().build();
        } else {
            errors += "Недостаточно прав для удаления шага CJ";
            logger.error("403 " + errors);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errors);
        }
    }

}
