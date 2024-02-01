package ru.beeline.cxbackend.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.domain.bi.BI;
import ru.beeline.cxbackend.domain.bi.ref.BIStatus;
import ru.beeline.cxbackend.dto.BIDto;
import ru.beeline.cxbackend.dto.BIEditabilityDto;
import ru.beeline.cxbackend.exception.StatusNotFoundException;
import ru.beeline.cxbackend.service.BusinessInteractionService;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/cx/v1/library/business-interactions")
@Api(value = "CX API", tags = "BI Library")
public class BIController {

    @Autowired
    private BusinessInteractionService businessInteractionService;

    @GetMapping
    @ApiOperation(value = "Получение BI по id продукта", response = List.class)
    public ResponseEntity<List<BIDto>> getBI(@RequestHeader("Authorization") String bearerToken, @RequestParam(value = "id_product", required = false) String idProduct) {
        return ResponseEntity.ok(businessInteractionService.getBI(idProduct));
    }

    @GetMapping("/find")
    @ApiOperation(value = "Получение BI продукта по фильтру", response = List.class)
    public ResponseEntity<List<BIDto>> getBIByFilter(@RequestHeader("Authorization") String bearerToken,
                                                     @RequestParam(required = false) String text,
                                                     @RequestParam(value = "id_product", required = false) String idProduct,
                                                     @RequestParam(value = "id_status", required = false) Long idStatus,
                                                     @RequestParam(value = "draft", required = false) Boolean isDraft) throws StatusNotFoundException {
        if(idStatus != null) {
            Optional<BIStatus> biStatus = businessInteractionService.getStatusById(idStatus);
            if(biStatus.isPresent()) return ResponseEntity.ok(businessInteractionService.getBIByFilter(text, idProduct, biStatus.get(), isDraft));
            else throw new StatusNotFoundException("id_status " + idStatus + " не найден");
        }
        return ResponseEntity.ok(businessInteractionService.getBIByFilter(text, idProduct, null, isDraft));
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Получение BI по id", response = List.class)
    public ResponseEntity<BIDto> getBIById(@RequestHeader("Authorization") String bearerToken, @PathVariable Long id) {
        return ResponseEntity.ok(businessInteractionService.getBIById(id));
    }

    @PostMapping
    @ApiOperation(value = "Добавление BI", response = List.class)
    public ResponseEntity<BIDto> createBI(@RequestHeader("authorization") String bearerToken, @RequestBody BI bi) {
        return ResponseEntity.ok(businessInteractionService.createBI(bi, bearerToken));
    }

    @PatchMapping("/{id}")
    @ApiOperation(value = "Редактирование BI", response = List.class)
    public ResponseEntity<BIDto> patchBI(@RequestHeader("authorization") String bearerToken,
                                         @RequestBody BI bi,
                                         @PathVariable Long id
                                         ) {
        return ResponseEntity.ok(businessInteractionService.patchBI(id, bi, bearerToken));
    }

    @GetMapping("editability/{id}")
    @ApiOperation(value = "Получение возможности редактирования BI", response = List.class)
    public ResponseEntity<BIEditabilityDto> getEditabilityDtoBI(@RequestHeader("Authorization") String bearerToken, @PathVariable Long id) {
        return ResponseEntity.ok(businessInteractionService.getEditabilityBI(id));
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "удаление BI по id", response = List.class)
    public ResponseEntity deleteBIById(@RequestHeader("authorization") String bearerToken,
                                       @PathVariable Long id) {
        businessInteractionService.deleteBIById(id, bearerToken);
        return ResponseEntity.ok(null);
    }
}