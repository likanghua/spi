package com.open.spi.factory.exception_advice;


import com.open.spi.common.exception.ExceptionResponse;
import com.open.spi.common.exception.MethodExecuteException;
import com.open.spi.common.exception.MethodNotFoundException;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionAdvice {

    @ExceptionHandler(MethodNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleException(MethodNotFoundException ex, Locale locale) {
        return handleMessage(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler({MethodExecuteException.class})
    public ResponseEntity<ExceptionResponse> handleException(Exception ex, Locale locale) {
        return handleMessage(HttpStatus.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handleException(IllegalArgumentException ex, Locale locale) {
        return handleMessage(HttpStatus.BAD_REQUEST, ex);
    }

/*
    private RestResponse handleMessage(String code, Object[] args, Locale locale) {
        return RestResponse
                .fail(code, messageSource
                        .getMessage(code, args, locale));
    }*/

    private ResponseEntity<ExceptionResponse> handleMessage(HttpStatus httpStatus, Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ExceptionResponse(ex.getClass().getCanonicalName(), ex.getMessage()));
    }


}
