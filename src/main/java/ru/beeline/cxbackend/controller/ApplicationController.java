/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.cxbackend.annotation.ApiStandardErrors;
import ru.beeline.cxbackend.annotation.CustomHeaders;

@RestController
@Tag(name = "Service", description = "Служебные методы сервиса (health-like/информационные ручки).")
public class ApplicationController {

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.name}")
    private String appName;


    @GetMapping("/")
    @CustomHeaders
    @ApiStandardErrors
    @Operation(
            summary = "Welcome",
            description = "Служебный метод: возвращает строку приветствия с именем приложения и версией."
    )
    public String getData() {
        return "Welcome " + appName + " " + appVersion;
    }

}
