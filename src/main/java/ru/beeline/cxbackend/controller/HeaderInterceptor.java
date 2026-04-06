/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.beeline.cxbackend.exception.ForbiddenException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static ru.beeline.cxbackend.utils.Constant.*;

public class HeaderInterceptor implements HandlerInterceptor {
    private Logger logger = LoggerFactory.getLogger(HeaderInterceptor.class);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            if(request.getRequestURI().contains("/actuator/prometheus")
                    || request.getRequestURI().contains("/swagger")
                    || request.getRequestURI().contains("/error")
                    || request.getRequestURI().contains("/v2/product/cj")
                    || request.getRequestURI().contains("/api-docs")
                    || request.getRequestURI().contains("/api/v1/cj/"))
            {
                return true;
            }
            Map<String, Object> headers = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                System.out.println(headerName + ": " + headerValue);
            }
            logger.info(USER_ID_HEADER + request.getHeader(USER_ID_HEADER));
            headers.put(USER_ID_HEADER, request.getHeader(USER_ID_HEADER).toString());
            logger.info(USER_PERMISSION_HEADER + toList(request.getHeader(USER_PERMISSION_HEADER)));
            headers.put(USER_PERMISSION_HEADER, toList(request.getHeader(USER_PERMISSION_HEADER).toString()));
            logger.info(USER_PRODUCTS_IDS_HEADER + toList(request.getHeader(USER_PRODUCTS_IDS_HEADER)));
            headers.put(USER_PRODUCTS_IDS_HEADER, toList(request.getHeader(USER_PRODUCTS_IDS_HEADER).toString()));
            logger.info(USER_ROLES_HEADER + toList(request.getHeader(USER_ROLES_HEADER)));
            headers.put(USER_ROLES_HEADER, toList(request.getHeader(USER_ROLES_HEADER).toString()));
            RequestContext.setHeaders(headers);
            logger.info("Set headers complete");
            return true;
        } catch (Exception e) {
            logger.info("403 Forbidden.");
            throw new ForbiddenException("403 Forbidden.");
        }
    }

    private List<String> toList(String value) {
        return Arrays.stream(value.split(","))
                .map(str -> str.substring(0))
                .map(str -> str.replaceAll("\"",""))
                .map(str -> str.replaceAll("]",""))
                .map(str -> str.replaceAll("\\[",""))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}