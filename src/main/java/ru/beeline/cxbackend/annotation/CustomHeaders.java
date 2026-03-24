/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.annotation;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static ru.beeline.cxbackend.utils.Constant.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiImplicitParams({
        @ApiImplicitParam(name = USER_ROLES_HEADER, paramType = "header", dataType = "string", dataTypeClass = String.class),
        @ApiImplicitParam(name = USER_ID_HEADER, paramType = "header", dataType = "string", dataTypeClass = String.class),
        @ApiImplicitParam(name = USER_PERMISSION_HEADER, paramType = "header", dataType = "string", dataTypeClass = String.class),
        @ApiImplicitParam(name = USER_PRODUCTS_IDS_HEADER, paramType = "header", dataType = "string", dataTypeClass = String.class)
})
public @interface CustomHeaders {
}
