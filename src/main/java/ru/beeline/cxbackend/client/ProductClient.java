/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.beeline.cxbackend.dto.GetProductsByIdsDTO;
import ru.beeline.cxbackend.dto.ProductInterfaceDTO;
import ru.beeline.cxbackend.dto.product.E2eCardDto;
import ru.beeline.cxbackend.dto.product.ProductOperationByTcItemDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;



@Slf4j
@Service
public class ProductClient {

    RestTemplate restTemplate;
    private final String productServerUrl;

    public ProductClient(@Value("${integration.product-server-url}") String productServerUrl,
                         RestTemplate restTemplate) {
        this.productServerUrl = productServerUrl;
        this.restTemplate = restTemplate;
    }

    public List<GetProductsByIdsDTO> getProductsByIds(List<Integer> ids) {
        try {
            if (ids.isEmpty()) {
                return new ArrayList<>();
            }
            log.info("response from Product ServerUrl: " + productServerUrl + "/api/v1/product/by-ids?ids=");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            ResponseEntity<List<GetProductsByIdsDTO>> response =
                    restTemplate.exchange(productServerUrl + "/api/v1/product/by-ids?ids=" + idsParam,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            new ParameterizedTypeReference<>() {
                            });
            return response.getBody();
        } catch (Exception e) {
            log.error("call's Exception " + e.getMessage());
            return null;
        }
    }

    public List<ProductOperationByTcItemDto> getOperationsByTechCapability(Integer tcId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<List<ProductOperationByTcItemDto>> response = restTemplate.exchange(
                    productServerUrl + "/api/v1/operation/tech-capability/" + tcId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {
                    });
            List<ProductOperationByTcItemDto> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Product returned 404 for tech-capability {}: {}", tcId, e.getMessage());
            return Collections.emptyList();
        } catch (HttpClientErrorException e) {
            log.error("Product client error for tech-capability {}: {}", tcId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Product service error", e);
        } catch (Exception e) {
            log.error("Product call failed for tech-capability {}: {}", tcId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service unavailable", e);
        }
    }

    public List<ProductInterfaceDTO> getProductsFromStructurizr(String cmdb) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<List<ProductInterfaceDTO>> response =
                    restTemplate.exchange(productServerUrl + "/api/v1/product/" + cmdb + "/interface/arch",
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            new ParameterizedTypeReference<>() {
                            });
            log.info("response from Product ServerUrl: " + productServerUrl + "/api/v1/product/" + cmdb + "/interface/arch");
            return response.getBody();
        } catch (Exception e) {
            log.error("call's Exception " + e.getMessage());
            return null;
        }
    }

    public List<E2eCardDto> getE2eWithBiStep() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String url = productServerUrl + "/api/v1/e2e?filter=with-bi-step";
            log.info("Запрос e2e в Product: {}", url);
            ResponseEntity<List<E2eCardDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {
                    });
            List<E2eCardDto> body = response.getBody();
            return body != null ? body : Collections.emptyList();
        } catch (HttpClientErrorException e) {
            log.error("Ошибка ответа Product при запросе e2e with-bi-step: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Product service error", e);
        } catch (Exception e) {
            log.error("Не удалось вызвать Product для e2e with-bi-step: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service unavailable", e);
        }
    }
}
