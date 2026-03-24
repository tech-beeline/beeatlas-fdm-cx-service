/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.beeline.cxbackend.dto.TcDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CapabilityClient {

    RestTemplate restTemplate;

    private final String capabilityServerUrl;

    public CapabilityClient(@Value("${integration.capability-server-url}") String capabilityServerUrl,
                            RestTemplate restTemplate) {
        this.capabilityServerUrl = capabilityServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<TcDTO> getTcs(List<Integer> tcIds) {
        List<Integer> filteredIds = tcIds.stream().filter(Objects::nonNull).toList();
        if (tcIds == null || tcIds.isEmpty() || filteredIds.isEmpty()) {
            log.debug("Список tcIds пустой — возврат пустого списка");
            return new ArrayList<>();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String tcIdsList = tcIds.stream()
                    .filter(Objects::nonNull)
                    .map(id -> "ids=" + id)
                    .collect(Collectors.joining("&"));
            String fullUrl = capabilityServerUrl + "/api/v1/tech-capabilities/list/by-ids?" + tcIdsList;
            log.debug("Запрос к сервису: {}", fullUrl);

            ResponseEntity<List<TcDTO>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<TcDTO>>() {
                    }
            );
            log.debug("Статус ответа: {}", response.getStatusCode());
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.error("Ошибка при получении TcDTO: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
