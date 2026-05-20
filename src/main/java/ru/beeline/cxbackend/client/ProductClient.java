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
import ru.beeline.cxbackend.controller.RequestContext;
import ru.beeline.cxbackend.dto.GetProductsByIdsDTO;
import ru.beeline.cxbackend.dto.ProductInterfaceDTO;
import ru.beeline.cxbackend.dto.product.ProductOperationByTcItemDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.beeline.cxbackend.utils.Constant.*;


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
            headers.set(USER_ID_HEADER, RequestContext.getUserId().toString());
            headers.set(USER_PERMISSION_HEADER, RequestContext.getUserPermissions().toString());
            headers.set(USER_PRODUCTS_IDS_HEADER, RequestContext.getUserProducts().toString());
            headers.set(USER_ROLES_HEADER, RequestContext.getUserRole().toString());
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
}
