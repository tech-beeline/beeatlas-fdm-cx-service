/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import ru.beeline.cxbackend.annotation.ApiErrorCodes;

@Component
public class ApiErrorCodesCustomizer implements OperationCustomizer {


    private static final int[] ALL_ERROR_CODES = {400, 401, 403, 404, 409, 422, 500, 503};

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {

        ApiErrorCodes annotation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), ApiErrorCodes.class);
        if (annotation == null) {
            annotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), ApiErrorCodes.class);
        }

        int[] errorCodes;
        if (annotation != null) {
            errorCodes = annotation.value();
        } else {
            errorCodes = ALL_ERROR_CODES;
        }
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        Schema<?> jsonErrorSchema = new Schema<>()
                .type("object")
                .addProperty("errorMessage", new StringSchema().description("Сообщение об ошибке"));

        Content jsonContent = new Content()
                .addMediaType("application/json", new MediaType().schema(jsonErrorSchema));
        for (int code : errorCodes) {
            String codeStr = String.valueOf(code);

            ApiResponse resp = new ApiResponse()
                    .description(getMessage(code))
                    .content(jsonContent);

            responses.addApiResponse(codeStr, resp);
        }

        return operation;
    }

    private String getMessage(int code) {
        switch (code) {
            case 400: return "Неверные входные данные";
            case 401: return "Требуется аутентификация";
            case 403: return "Доступ запрещен / отсутствуют обязательные заголовки";
            case 404: return "Ресурс не найден";
            case 409: return "Конфликт данных";
            case 422: return "Ошибка валидации";
            case 500: return "Внутренняя ошибка сервера";
            case 503: return "Сервис недоступен";
            default:  return "Ошибка " + code;
        }
    }
}