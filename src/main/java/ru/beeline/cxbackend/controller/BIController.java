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
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.dto.BIDto;
import ru.beeline.cxbackend.dto.BIEditabilityDto;
import ru.beeline.cxbackend.exception.BINotExistException;
import ru.beeline.cxbackend.exception.StatusNotFoundException;
import ru.beeline.cxbackend.service.BusinessInteractionService;

import java.util.List;

import static ru.beeline.cxbackend.controller.RequestContext.getUserPermissions;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping(value = "/api/cx/v1/library/business-interactions")
@Api(value = "CX API", tags = "BI Library")
public class BIController {
    private Logger logger = LoggerFactory.getLogger(BIController.class);

    @Autowired
    private BusinessInteractionService businessInteractionService;

    @GetMapping
    @ApiOperation(value = "Получение BI по id продукта", response = List.class)
    public ResponseEntity getBI(@RequestParam(value = "id_product", required = false) Long idProduct) {
        return ResponseEntity.ok(businessInteractionService.getBI(idProduct));
    }

    @GetMapping("/find")
    @ApiOperation(value = "Получение BI продукта по фильтру", response = List.class)
    public ResponseEntity<List<BIDto>> getBIByFilter(@RequestParam(required = false) String text,
                                                     @RequestParam(value = "id_product", required = false) Long idProduct,
                                                     @RequestParam(value = "id_status", required = false) Long idStatus,
                                                     @RequestParam(value = "draft", required = false) Boolean isDraft) throws StatusNotFoundException {
        if (idStatus != null) {
            return businessInteractionService.getStatusById(idStatus)
                    .map(biStatus -> ResponseEntity.ok(businessInteractionService.getBIByFilter(text, idProduct, biStatus, isDraft)))
                    .orElseThrow(() -> new StatusNotFoundException("id_status " + idStatus + " is not found"));
        }
        return ResponseEntity.ok(businessInteractionService.getBIByFilter(text, idProduct, null, isDraft));
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Получение BI по id", response = List.class)
    public ResponseEntity<BIDto> getBIById(@PathVariable Long id) throws BINotExistException {
        return ResponseEntity.ok(businessInteractionService.getBIById(id));
    }

    @PostMapping
    @ApiOperation(value = "Добавление BI", response = List.class)
    public ResponseEntity createBI(@RequestBody BI bi) {
        if ((getUserPermissions()).contains(Permission.PermissionType.CREATE_ARTIFACT.toString())) {
            return ResponseEntity.ok(businessInteractionService.createBI(bi));
        } else {
            logger.error("403 Недостаточно прав для создания BI");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Недостаточно прав для создания BI");
        }

    }

    @PatchMapping("/{id}")
    @ApiOperation(value = "Редактирование BI", response = List.class)
    public ResponseEntity patchBI(@RequestBody BI bi,
                                  @PathVariable Long id
    ) {
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            return ResponseEntity.ok(businessInteractionService.patchBI(id, bi));
        } else {
            logger.error("403 Недостаточно прав для редактирования BI");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Недостаточно прав для редактирования BI");
        }
    }

    @GetMapping("editability/{id}")
    @ApiOperation(value = "Получение возможности редактирования BI", response = List.class)
    public ResponseEntity<BIEditabilityDto> getEditabilityDtoBI(@PathVariable Long id) {
        return ResponseEntity.ok(businessInteractionService.getEditabilityBI(id));
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "удаление BI по id", response = List.class)
    public ResponseEntity deleteBIById(@PathVariable Long id) {

        if ((getUserPermissions()).contains(Permission.PermissionType.DELETE_ARTIFACT.toString())) {
            businessInteractionService.deleteBIById(id);
            return ResponseEntity.ok(null);
        } else {
            logger.error("403 Недостаточно прав для удаления BI");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Недостаточно прав для удаления BI");
        }
    }
}