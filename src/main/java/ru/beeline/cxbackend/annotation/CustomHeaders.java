/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.annotation;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static ru.beeline.cxbackend.utils.Constant.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Parameters({
        @Parameter(
                name = USER_ID_HEADER,
                in = ParameterIn.HEADER,
                required = true,
                description = "Идентификатор пользователя.",
                schema = @Schema(type = "string", example = "12345")
        ),
        @Parameter(
                name = USER_ROLES_HEADER,
                in = ParameterIn.HEADER,
                required = true,
                description = "Роли пользователя (список через запятую).",
                schema = @Schema(type = "string", example = "ADMINISTRATOR,PRODUCT_OWNER")
        ),
        @Parameter(
                name = USER_PERMISSION_HEADER,
                in = ParameterIn.HEADER,
                required = true,
                description = "Права пользователя (список через запятую).",
                schema = @Schema(type = "string", example = "CREATE_ARTIFACT,EDIT_ARTIFACT,DELETE_ARTIFACT,DESIGN_ARTIFACT")
        ),
        @Parameter(
                name = USER_PRODUCTS_IDS_HEADER,
                in = ParameterIn.HEADER,
                required = true,
                description = "Список id продуктов, к которым у пользователя есть доступ (через запятую).",
                schema = @Schema(type = "string", example = "1,2,3")
        )
})
public @interface CustomHeaders {
}
