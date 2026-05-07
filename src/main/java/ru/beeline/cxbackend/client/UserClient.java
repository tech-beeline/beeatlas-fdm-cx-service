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
import ru.beeline.cxbackend.dto.AuthUserDto;
import ru.beeline.cxbackend.dto.UserInfoDto;
import ru.beeline.cxbackend.dto.UserProfileDto;
import ru.beeline.cxbackend.exception.NotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserClient {

    RestTemplate restTemplate;
    private final String userServerUrl;

    public UserClient(@Value("${integration.auth-server-url}") String userServerUrl, RestTemplate restTemplate) {
        this.userServerUrl = userServerUrl;
        this.restTemplate = restTemplate;
    }

    public UserProfileDto getUserProfile(Long id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(userServerUrl + "/api/v1/user/" + id,
                    HttpMethod.GET, entity, new ParameterizedTypeReference<UserProfileDto>() {
                    }).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            String message = e.getResponseBodyAsString();
            throw new NotFoundException(message);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public List<AuthUserDto> getUsersByIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }

        List<Long> cleaned = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (cleaned.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String joined = cleaned.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            return restTemplate.exchange(
                    userServerUrl + "/api/v1/user?ids=" + joined,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<AuthUserDto>>() {
                    }
            ).getBody();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    public UserInfoDto getUserInfo(Long userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            return restTemplate.exchange(
                    userServerUrl + "/api/admin/v1/user/" + userId + "/user-info",
                    HttpMethod.GET, entity, new ParameterizedTypeReference<UserInfoDto>() {}
            ).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public boolean userExists(Long id) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            restTemplate.exchange(userServerUrl + "/api/v1/user/" + id,
                    HttpMethod.GET, entity, new ParameterizedTypeReference<UserProfileDto>() {
                    });
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
