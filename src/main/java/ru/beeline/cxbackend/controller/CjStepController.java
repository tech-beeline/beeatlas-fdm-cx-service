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
import ru.beeline.cxbackend.exception.*;
import ru.beeline.cxbackend.service.CJService;
import ru.beeline.cxbackend.service.CJStepService;

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
       return ResponseEntity.ok(cjStepService.getStepByCJId(id));
    }

    @PostMapping("/api/cx/v1/product/cj/{id}/step")
    @ResponseBody
    @ApiOperation(value = "Добавление шага в коллекцию шагов CJ")
    public ResponseEntity addCJStep(@PathVariable Long id,
                                    @RequestBody CjStepDto cjStepDto){
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            throw new NotFoundException("CJ с id = " + id + " не найден");
        }
        validateAccessProduct(getUserPermissions(),
                getUserProducts(),
                currentCJ.getIdProductExt());
        if ((getUserPermissions()).contains(Permission.PermissionType.CREATE_ARTIFACT.toString())) {
            if (currentCJ.isBDraft()) {
                return ResponseEntity.ok(cjStepService.addStep(id, cjStepDto));
            } else {
                throw new ConflictException("CJ с id = " + id + " находится в статусе Опубликован. Добавление шага невозможно.");
            }
        } else {
            throw new ForbiddenException("Недостаточно прав для добавления шага CJ");
        }
    }

    @GetMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @ApiOperation(value = "Получение шага CJ по id")
    public ResponseEntity getCJStepById(@PathVariable Long id) {
        return ResponseEntity.ok(cjStepService.getStepById(id));
    }

    @PatchMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @ApiOperation(value = "Изменение шага CJ")
    public ResponseEntity updateCJStep(@PathVariable Long id, @RequestBody CjStepDto cjStepDto) {
        CJStep cjStep = cjStepService.getStepById(id);
        Long productId = cjService.getById(cjStep.getCjId()).getIdProductExt();
        validateAccessProduct(getUserPermissions(),
                getUserProducts(),
                productId);

        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            return ResponseEntity.ok(cjStepService.updateStep(cjStep, cjStepDto));
        } else {
            throw new ForbiddenException("Недостаточно прав для изменения шага CJ");
        }
    }

    @DeleteMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @ApiOperation(value = "Удаление шага CJ")
    public ResponseEntity deleteCJStep(@PathVariable Long id){
                CJStep cjStep = cjStepService.getStepById(id);
        Long productId = cjService.getById(cjStep.getCjId()).getIdProductExt();
        validateAccessProduct(getUserPermissions(),
                getUserProducts(), productId);

        if ((getUserPermissions()).contains(Permission.PermissionType.DELETE_ARTIFACT.toString())) {
            cjStepService.deleteStep(cjStep);
            return ResponseEntity.ok().build();
        } else {
            throw new ForbiddenException("Недостаточно прав для удаления шага CJ");
        }
    }

}
