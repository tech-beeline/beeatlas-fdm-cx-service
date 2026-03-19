/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.builders.ResponseBuilder;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(getApiInfo())
                .useDefaultResponseMessages(false)
                .globalResponses(HttpMethod.GET, getErrorResponses())
                .globalResponses(HttpMethod.POST, getGlobalErrorResponses())
                .globalResponses(HttpMethod.PUT, getGlobalErrorResponses())
                .globalResponses(HttpMethod.DELETE, getGlobalErrorResponses())
                .globalResponses(HttpMethod.PATCH, getErrorResponses())
                .securitySchemes(List.of(apiKey()))
                .securityContexts(List.of(securityContext()))
                .select()
                .apis(RequestHandlerSelectors.basePackage("ru.beeline.cxbackend.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    private List<Response> getGlobalErrorResponses() {
        return Arrays.asList(
                new ResponseBuilder().code("400").description("Неверные входные данные").build(),
                new ResponseBuilder().code("401").description("Требуется аутентификация").build(),
                new ResponseBuilder().code("403").description("Доступ запрещен").build(),
                new ResponseBuilder().code("404").description("Ресурс не найден").build(),
                new ResponseBuilder().code("409").description("Конфликт данных").build(),
                new ResponseBuilder().code("500").description("Внутренняя ошибка сервера").build()

        );
    }

    private List<Response> getErrorResponses() {
        return Arrays.asList(
                new ResponseBuilder().code("400").description("Неверные входные данные").build(),
                new ResponseBuilder().code("401").description("Требуется аутентификация").build(),
                new ResponseBuilder().code("500").description("Внутренняя ошибка сервера").build()

        );
    }

    private ApiKey apiKey() {
        return new ApiKey("Bearer", "Authorization", "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(defaultAuth()).build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[]{authorizationScope};
        return List.of(new SecurityReference("Bearer", authorizationScopes));
    }

    private ApiInfo getApiInfo() {
        return new ApiInfo(
                "Cx service API",
                "API документация",
                "1.0",
                "Terms of service",
                new Contact("Example", "www.example.com", "example@company.com"),
                "License of API", "API license URL", Collections.emptyList());
    }
}
