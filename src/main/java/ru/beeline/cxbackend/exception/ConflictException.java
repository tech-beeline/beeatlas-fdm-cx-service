package ru.beeline.cxbackend.exception;


public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}