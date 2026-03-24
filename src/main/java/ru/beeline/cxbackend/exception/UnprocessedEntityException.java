/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.exception;


public class UnprocessedEntityException extends RuntimeException {
    public UnprocessedEntityException(String message) {
        super(message);
    }
}