/*
 * Copyright (c) 2024 PJSC VimpelCom
 */
package ru.beeline.cxbackend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "CX Service API",
                description = """
                        Сервис клиентского опыта (CX): управление Customer Journey (CJ) и Business Interaction (BI).
                        """,
                version = "${app.version}",
                contact = @Contact(name = "CX backend"),
                license = @License(name = "Proprietary")
        ),
        tags = {
                @Tag(name = "CJ", description = "Customer Journey (CJ): список/получение/создание/изменение/удаление CJ, импорт/обновление из BPMN, алерты."),
                @Tag(name = "CJ Step", description = "Шаги Customer Journey (CJ): список шагов, получение шага, добавление/изменение/удаление."),
                @Tag(name = "BI Library", description = "Библиотека Business Interaction (BI): поиск/получение/создание/редактирование/удаление BI и редактирование шагов BI и их связей."),
                @Tag(name = "BI by CjStep", description = "Привязка BI к шагам CJ: BI по шагу CJ, CJ по BI, привязка/отвязка BI."),
                @Tag(name = "BI References", description = "Справочники BI: чувства, статусы, каналы, участники."),
                @Tag(name = "Tech Capability", description = "Связь технических возможностей (TC) с CJ через операции продукта и шаги BI/CJ.")
        }
)
public class OpenApiConfig {
}

