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
import ru.beeline.cxbackend.dto.BIPostDto;
import ru.beeline.cxbackend.dto.BIV2Dto;
import ru.beeline.cxbackend.exception.ForbiddenException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.service.BusinessInteractionService;

import java.util.List;

import static ru.beeline.cxbackend.controller.RequestContext.getUserPermissions;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping(value = "/api/cx")
@Api(value = "CX API", tags = "BI Library")
public class BIController {
    private Logger logger = LoggerFactory.getLogger(BIController.class);

    @Autowired
    private BusinessInteractionService businessInteractionService;

    @GetMapping("/v1/library/business-interactions")
    @ApiOperation(value = "Получение BI по id продукта", response = List.class)
    public ResponseEntity getBI(@RequestParam(value = "id_product", required = false) Long idProduct) {
        return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.getBI(idProduct));
    }

    @GetMapping("/v1/library/business-interactions/find")
    @ApiOperation(value = "Получение BI продукта по фильтру", response = List.class)
    public ResponseEntity<List<BIDto>> getBIByFilter(@RequestParam(required = false) String text,
                                                     @RequestParam(value = "id_product", required = false) Long idProduct,
                                                     @RequestParam(value = "id_status", required = false) Long idStatus,
                                                     @RequestParam(value = "draft", required = false) Boolean isDraft) {
        if (idStatus != null) {
            return businessInteractionService.getStatusById(idStatus)
                    .map(biStatus -> ResponseEntity.status(HttpStatus.OK)
                            .body(businessInteractionService.getBIByFilter(text, idProduct, biStatus, isDraft))
                    )
                    .orElseThrow(() -> new NotFoundException("id_status " + idStatus + " is not found"));
        }
        return ResponseEntity.status(HttpStatus.OK)
                .body(businessInteractionService.getBIByFilter(text, idProduct, null, isDraft));
    }

    @GetMapping("/v1/library/business-interactions/{id}")
    @ApiOperation(value = "Получение BI по id", response = List.class)
    public ResponseEntity<BIDto> getBIById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.getBIById(id));
    }

    @GetMapping("/v2/library/business-interactions/{id}")
    @ApiOperation(value = "Получение BI по id v2", response = List.class)
    public ResponseEntity<BIV2Dto> getBIByIdV2(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.getBIByIdV2(id));
    }

    @PostMapping("/v1/library/business-interactions")
    @ApiOperation(value = "Добавление BI", response = List.class)
    public ResponseEntity createBI(@RequestBody BIPostDto bi) {
        if ((getUserPermissions()).contains(Permission.PermissionType.CREATE_ARTIFACT.toString())) {
            return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.createBI(bi));
        } else {
            throw new ForbiddenException("Недостаточно прав для создания BI");
        }
    }

    @PatchMapping("/v1/library/business-interactions/{id}")
    @ApiOperation(value = "Редактирование BI", response = List.class)
    public ResponseEntity patchBI(@RequestBody BI bi,
                                  @PathVariable Long id
    ) {
        if ((getUserPermissions()).contains(Permission.PermissionType.EDIT_ARTIFACT.toString())) {
            return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.patchBI(id, bi));
        } else {
            throw new ForbiddenException("Недостаточно прав для редактирования BI");
        }
    }

    @GetMapping("/v1/library/business-interactions/editability/{id}")
    @ApiOperation(value = "Получение возможности редактирования BI", response = List.class)
    public ResponseEntity<BIEditabilityDto> getEditabilityDtoBI(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(businessInteractionService.getEditabilityBI(id));
    }

    @DeleteMapping("/v1/library/business-interactions/{id}")
    @ApiOperation(value = "удаление BI по id", response = List.class)
    public ResponseEntity deleteBIById(@PathVariable Long id) {
        if ((getUserPermissions()).contains(Permission.PermissionType.DELETE_ARTIFACT.toString())) {
            businessInteractionService.deleteBIById(id);
            return ResponseEntity.status(HttpStatus.OK).build();
        } else {
            throw new ForbiddenException("Недостаточно прав для удаления BI");
        }
    }
}