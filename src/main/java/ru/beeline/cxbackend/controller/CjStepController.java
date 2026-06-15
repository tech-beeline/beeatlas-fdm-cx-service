/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.beeline.cxbackend.annotation.ApiStandardErrors;
import ru.beeline.cxbackend.annotation.CustomHeaders;
import ru.beeline.cxbackend.domain.cj.CJ;
import ru.beeline.cxbackend.domain.cj.CJStep;
import ru.beeline.cxbackend.dto.CjStepDto;
import ru.beeline.cxbackend.dto.CjStepFullDto;
import ru.beeline.cxbackend.exception.ConflictException;
import ru.beeline.cxbackend.exception.NotFoundException;
import ru.beeline.cxbackend.service.CJService;
import ru.beeline.cxbackend.service.CJStepService;

@RestController
@RequestMapping
@Tag(
        name = "CJ Step",
        description = "Шаги Customer Journey (CJ): получение списка шагов, получение шага, добавление/изменение/удаление шага CJ."
)
public class CjStepController {

    @Autowired
    private CJStepService cjStepService;

    @Autowired
    private CJService cjService;

    @GetMapping("/api/cx/v1/product/cj/{id}/step")
    @ResponseBody
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Список шагов CJ", description = "Возвращает коллекцию шагов CJ по id CJ.")
    @ApiResponse(
            responseCode = "200",
            description = "Список шагов CJ",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CjStepFullDto.class)))
    )
    public ResponseEntity<java.util.List<CjStepFullDto>> getCJSteps(@PathVariable Long id) {
        return ResponseEntity.ok(cjStepService.getStepByCJId(id));
    }

    @GetMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Шаг CJ по id", description = "Возвращает шаг CJ (расширенное представление) по id шага.")
    @ApiResponse(
            responseCode = "200",
            description = "Шаг CJ",
            content = @Content(schema = @Schema(implementation = CjStepFullDto.class))
    )
    public ResponseEntity<CjStepFullDto> getCJStepById(@PathVariable Long id) {
        return ResponseEntity.ok(cjStepService.getStepFullDto(id));
    }


    @PostMapping("/api/cx/v1/product/cj/{id}/step")
    @ResponseBody
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "Добавление шага CJ",
            description = "Добавляет шаг в CJ. Требуется право `CREATE_ARTIFACT`, доступ к продукту и CJ должен быть в черновике."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Созданный шаг CJ",
            content = @Content(schema = @Schema(implementation = CjStepFullDto.class))
    )
    public ResponseEntity<CjStepFullDto> addCJStep(@PathVariable Long id,
                                                   @RequestBody CjStepDto cjStepDto) {
        CJ currentCJ = cjService.getById(id);
        if (currentCJ == null) {
            throw new NotFoundException("CJ с id = " + id + " не найден");
        }
        if (currentCJ.isBDraft()) {
            return ResponseEntity.ok((CjStepFullDto) cjStepService.addStep(id, cjStepDto));
        } else {
            throw new ConflictException("CJ с id = " + id + " находится в статусе Опубликован. Добавление шага невозможно.");
        }
    }

    @PatchMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Изменение шага CJ", description = "Обновляет данные шага CJ. Требуется право `EDIT_ARTIFACT` и доступ к продукту.")
    @ApiResponse(
            responseCode = "200",
            description = "Обновлённый шаг CJ",
            content = @Content(schema = @Schema(implementation = CjStepFullDto.class))
    )
    public ResponseEntity<CjStepFullDto> updateCJStep(@PathVariable Long id, @RequestBody CjStepDto cjStepDto) {
        CJStep cjStep = cjStepService.getStepById(id);
        return ResponseEntity.ok(cjStepService.updateStep(cjStep, cjStepDto));
    }

    @DeleteMapping("/api/cx/v1/product/cj/step/{id}")
    @ResponseBody
    @CustomHeaders
    @ApiStandardErrors
    @Operation(summary = "Удаление шага CJ", description = "Удаляет шаг CJ. Требуется право `DELETE_ARTIFACT` и доступ к продукту.")
    @ApiResponse(responseCode = "200", description = "Шаг удалён")
    public ResponseEntity<Void> deleteCJStep(@PathVariable Long id) {
        CJStep cjStep = cjStepService.getStepById(id);
        cjStepService.deleteStep(cjStep);
        return ResponseEntity.ok().build();
    }
}
