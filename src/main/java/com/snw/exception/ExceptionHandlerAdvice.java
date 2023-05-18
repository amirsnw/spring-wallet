package com.snw.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.sql.SQLException;
import java.util.Date;


@ControllerAdvice
@RestController
public class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<String> handlePessimisticLockException(TransactionSystemException ex, WebRequest request) {
        Throwable cause = ex.getRootCause();

        if (cause instanceof SQLException) {
            ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(),
                    "The entity has been locked by another transaction",
                    cause.getMessage());
            return new ResponseEntity(exceptionResponse, HttpStatus.CONFLICT);
        } else {
            return handleAllException(ex, request);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FinancialBoundaryException.class)
    public ResponseEntity<String> handleFinancialBoundaryException(FinancialBoundaryException ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity(ex.getConstraintViolationMap(), headers, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoRecordFoundException.class)
    public ResponseEntity<String> handleNoRecordFoundException(NoRecordFoundException ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity(exceptionResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllException(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = new ExceptionResponse(new Date(), ex.getMessage(),
                request.getDescription(false));
        return new ResponseEntity(exceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
