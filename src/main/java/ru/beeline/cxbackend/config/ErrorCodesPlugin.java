/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.config;

import org.springframework.stereotype.Component;
import ru.beeline.cxbackend.annotation.ApiErrorCodes;
import springfox.documentation.builders.ResponseBuilder;
import springfox.documentation.service.Response;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;

import java.util.HashSet;
import java.util.Set;

@Component
public class ErrorCodesPlugin implements OperationBuilderPlugin {

    @Override
    public void apply(OperationContext context) {
        ApiErrorCodes annotation = context.findAnnotation(ApiErrorCodes.class).orElse(null);

        if (annotation != null) {
            Set<Response> responses = new HashSet<>();
            if (context.operationBuilder().build().getResponses() != null) {
                responses.addAll(context.operationBuilder().build().getResponses());
            }
            for (int code : annotation.value()) {
                responses.add(new ResponseBuilder()
                        .code(String.valueOf(code))
                        .description(getMessage(code))
                        .build());
            }
            context.operationBuilder().responses(responses);
        }
    }

    private String getMessage(int code) {
        switch (code) {
            case 400:
                return "Неверные входные данные";
            case 401:
                return "Требуется аутентификация";
            case 403:
                return "Доступ запрещен";
            case 404:
                return "Ресурс не найден";
            case 409:
                return "Конфликт данных";
            case 500:
                return "Внутренняя ошибка сервера";
            default:
                return "Error " + code;
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }
}