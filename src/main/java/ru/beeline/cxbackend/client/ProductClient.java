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
import ru.beeline.cxbackend.controller.RequestContext;
import ru.beeline.cxbackend.dto.GetProductsByIdsDTO;
import ru.beeline.cxbackend.dto.ProductInterfaceDTO;

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
            HttpHeaders headers = new HttpHeaders();
            headers.set(USER_ID_HEADER, RequestContext.getUserId().toString());
            headers.set(USER_PERMISSION_HEADER, RequestContext.getUserPermissions().toString());
            headers.set(USER_PRODUCTS_IDS_HEADER, RequestContext.getUserProducts().toString());
            headers.set(USER_ROLES_HEADER, RequestContext.getUserRole().toString());
            headers.setContentType(MediaType.APPLICATION_JSON);
            String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            ResponseEntity<List<GetProductsByIdsDTO>> response =
                    restTemplate.exchange(productServerUrl + "/api/v1/product/by-ids?ids=" + idsParam,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            new ParameterizedTypeReference<>() {
                            });
            log.info("response from Product ServerUrl: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("call's Exception " + e.getMessage());
            return null;
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
            log.info("response from Product ServerUrl: " + response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("call's Exception " + e.getMessage());
            return null;
        }
    }
}
