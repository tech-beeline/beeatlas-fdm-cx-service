package ru.beeline.cxbackend.exception;


public class UnprocessedEntityException extends RuntimeException {
    public UnprocessedEntityException(String message) {
        super(message);
    }
}