/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.cxbackend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.beeline.cxbackend.dto.owners.ErrorMessageDto;
import ru.beeline.cxbackend.exception.AuthServiceException;
import ru.beeline.cxbackend.exception.*;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error(e.getMessage());
        String message = "doc-id".equals(e.getName())
                ? "Неверный формат идентификатора документа"
                : e.getMessage();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(ErrorMessageDto.builder().errorMessage(message).build());
    }

    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<Object> handleException(AuthServiceException e) {
        log.error(e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(ErrorMessageDto.builder().errorMessage(e.getMessage()).build());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleException(NotFoundException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(ErrorMessageDto.builder().errorMessage(e.getMessage()).build());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleException(BadRequestException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(ErrorMessageDto.builder().errorMessage(e.getMessage()).build());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleException(ForbiddenException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(ErrorMessageDto.builder().errorMessage(e.getMessage()).build());
    }

    @ExceptionHandler(UnprocessedEntityException.class)
    public ResponseEntity<Object> handleException(UnprocessedEntityException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(ErrorMessageDto.builder().errorMessage(e.getMessage()).build());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleException(ConflictException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(ErrorMessageDto.builder().errorMessage(e.getMessage()).build());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleException(RuntimeException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(ErrorMessageDto.builder().errorMessage(e.getMessage()).build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleException(IllegalArgumentException e) {
        log.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("content-type", MediaType.APPLICATION_JSON_VALUE)
                .body(ErrorMessageDto.builder().errorMessage("400 Bad Request : " + e.getMessage()).build());
    }
}