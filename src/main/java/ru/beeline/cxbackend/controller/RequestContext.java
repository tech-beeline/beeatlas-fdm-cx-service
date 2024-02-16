package ru.beeline.cxbackend.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static ru.beeline.cxbackend.utils.Constant.USER_PERMISSION_HEADER;
import static ru.beeline.cxbackend.utils.Constant.USER_PRODUCTS_IDS_HEADER;

public class RequestContext {
    private static final ThreadLocal<Map<String, Object>> headersThreadLocal = new ThreadLocal<>();

    public static void setHeaders(Map<String, Object> headers) {
        headersThreadLocal.set(headers);
    }

    public static Map<String, Object> getHeaders() {
        return headersThreadLocal.get();
    }

    public static List<String> getUserPermissions() {
        return (List<String>) getHeaders().get(USER_PERMISSION_HEADER);
    }
    public static List<Long> getUserProducts() {
        return (List<Long>) getHeaders().get(USER_PRODUCTS_IDS_HEADER);
    }
}
