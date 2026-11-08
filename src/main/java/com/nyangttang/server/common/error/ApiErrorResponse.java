package com.nyangttang.server.common.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(Detail error) {
    public record Detail(String code,String message,int status,String path,Instant timestamp,String requestId,List<FieldError> fieldErrors) {}
    public record FieldError(String field,String message) {}
}
