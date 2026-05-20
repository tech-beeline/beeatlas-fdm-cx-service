/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.cxbackend.annotation;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Неверные входные данные",
                content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "Требуется аутентификация (на уровне gateway/окружения)",
                content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён / отсутствуют обязательные заголовки",
                content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "404", description = "Ресурс не найден",
                content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "409", description = "Конфликт данных",
                content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "422", description = "Невалидная сущность (Unprocessable Entity)",
                content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                content = @Content(schema = @Schema(implementation = String.class)))
})
public @interface ApiStandardErrors {
}

