/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.cxbackend.dto.DocumentationTypeDTO;
import ru.beeline.cxbackend.exception.NotFoundException;

import java.util.List;

import static ru.beeline.cxbackend.utils.Constant.USER_ID_HEADER;

@Slf4j
@Service
public class DocumentClient {

    private final RestTemplate restTemplate;

    private final String documentServiceUrl;

    public DocumentClient(@Value("${integration.document-server-url}") String documentServiceUrl,
                          RestTemplate restTemplate) {
        this.documentServiceUrl = documentServiceUrl;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<byte[]> getDocument(Long docId, Integer documentationTypeId, Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (userId != null) {
                headers.set(USER_ID_HEADER, userId.toString());
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.info("get document: /api/v1/documents/" + documentationTypeId + "/" + docId);
            return restTemplate.exchange(documentServiceUrl + "/api/v1/documents/" + documentationTypeId + "/" + docId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<byte[]>() {
                    });
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Запись с данным id не найдена: ", e);
            throw new NotFoundException("Документ не найден");
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw e;
        }
    }

    public List<DocumentationTypeDTO> getDocumentationType(String entityType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.info("get Documentation Type: /api/v1/documentations/" + entityType);
            return restTemplate.exchange(documentServiceUrl + "/api/v1/documentations/" + entityType,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<DocumentationTypeDTO>>() {
                    }).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Запись с  не entityType: {} найдена: ", entityType, e);
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw e;
        }
    }
}
