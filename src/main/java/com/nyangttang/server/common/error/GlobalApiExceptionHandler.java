package com.nyangttang.server.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalApiExceptionHandler extends ResponseEntityExceptionHandler {
    private final ApiErrors errors;
    public GlobalApiExceptionHandler(ApiErrors errors){this.errors=errors;}
    @Override protected ResponseEntity<Object> handleExceptionInternal(Exception ex,Object ignored,HttpHeaders headers,HttpStatusCode status,WebRequest request){
        var servlet=(ServletWebRequest)request;
        if(servlet.getResponse()!=null && servlet.getResponse().isCommitted())return null;
        return errors.response(ex,status.value(),headers,servlet.getRequest());
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception ex,HttpServletRequest request){
        int status=500;
        if(ex instanceof jakarta.validation.ConstraintViolationException)status=400;
        else if(ex instanceof org.springframework.dao.DataIntegrityViolationException
                || ex instanceof org.springframework.dao.OptimisticLockingFailureException
                || ex instanceof org.springframework.dao.PessimisticLockingFailureException)status=409;
        else if(ex instanceof org.springframework.dao.DataAccessResourceFailureException
                || ex instanceof org.springframework.transaction.CannotCreateTransactionException)status=503;
        return errors.response(ex,status,new HttpHeaders(),request);
    }
}
