package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.dto.BIDto;
import ru.beeline.cxbackend.dto.BIEditabilityDto;
import ru.beeline.cxbackend.exception.BINotExistException;
import ru.beeline.cxbackend.exception.StatusNotFoundException;
import ru.beeline.cxbackend.service.BusinessInteractionService;

import java.util.List;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping(value = "/api/cx/v1/library/business-interactions")
@Api(value = "CX API", tags = "BI Library")
public class BIController {

    @Autowired
    private BusinessInteractionService businessInteractionService;

    @GetMapping
    @ApiOperation(value = "Получение BI по id продукта", response = List.class)
    public ResponseEntity<List<BIDto>> getBI(@RequestParam(value = "id_product", required = false) Long idProduct) {
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
    public ResponseEntity<BIDto> createBI(@RequestBody BI bi) {
        return ResponseEntity.ok(businessInteractionService.createBI(bi));
    }

    @PatchMapping("/{id}")
    @ApiOperation(value = "Редактирование BI", response = List.class)
    public ResponseEntity<BIDto> patchBI(@RequestBody BI bi,
                                         @PathVariable Long id
    ) {
        return ResponseEntity.ok(businessInteractionService.patchBI(id, bi));
    }

    @GetMapping("editability/{id}")
    @ApiOperation(value = "Получение возможности редактирования BI", response = List.class)
    public ResponseEntity<BIEditabilityDto> getEditabilityDtoBI(@PathVariable Long id) {
        return ResponseEntity.ok(businessInteractionService.getEditabilityBI(id));
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "удаление BI по id", response = List.class)
    public ResponseEntity deleteBIById(@PathVariable Long id) {
        businessInteractionService.deleteBIById(id);
        return ResponseEntity.ok(null);
    }
}