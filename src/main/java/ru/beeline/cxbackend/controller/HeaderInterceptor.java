package ru.beeline.cxbackend.controller;

import org.springframework.web.servlet.HandlerInterceptor;
import ru.beeline.cxbackend.exception.UnauthorizedException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.beeline.cxbackend.utils.Constant.*;

public class HeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(USER_ID_HEADER, request.getHeader(USER_ID_HEADER));
            headers.put(USER_PERMISSION_HEADER, toList(request.getHeader(USER_PERMISSION_HEADER)));
            headers.put(USER_PRODUCTS_IDS_HEADER, toList(request.getHeader(USER_PRODUCTS_IDS_HEADER)));
            headers.put(USER_ROLES_HEADER, toList(request.getHeader(USER_ROLES_HEADER)));
            RequestContext.setHeaders(headers);
        } catch (Exception e) {
            new UnauthorizedException("401 " + "Создавать CJ могут только авторизованные пользователи");
        }
        return true;
    }

    private List<String> toList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }
}