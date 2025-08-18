package ru.beeline.cxbackend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.beeline.cxbackend.controller.RequestContext;
import ru.beeline.cxbackend.exception.NotFoundException;

import static ru.beeline.cxbackend.utils.Constant.USER_ID_HEADER;
import static ru.beeline.cxbackend.utils.Constant.USER_ROLES_HEADER;

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

    public ResponseEntity<byte[]> getDocument(Long docId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(USER_ID_HEADER, RequestContext.getUserId().toString());
            headers.set(USER_ROLES_HEADER, RequestContext.getUserRole().toString());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(documentServiceUrl + "/api/v1/documents/cj" + docId,
                                         HttpMethod.GET,
                                         entity,
                                         new ParameterizedTypeReference<byte[]>() {});
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Запись с данным id не найдена: ", e);
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            log.error("Exception occurred: ", e);
            throw e;
        }
    }
}
