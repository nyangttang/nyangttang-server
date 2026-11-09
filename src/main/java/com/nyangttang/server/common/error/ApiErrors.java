package com.nyangttang.server.common.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.*;
import org.springframework.web.method.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.beans.TypeMismatchException;
import java.time.Instant;
import java.util.*;

@Component
public class ApiErrors {
    public ResponseEntity<Object> response(Exception exception,int status,HttpHeaders original,HttpServletRequest request){
        String code=code(status),message=message(status);
        List<ApiErrorResponse.FieldError> fields=List.of();
        if(exception instanceof ApiException api){code=api.getCode();if(status<500)message=api.getReason();}
        else if(exception instanceof MethodArgumentNotValidException validation){
            code="VALIDATION_FAILED";message="입력값을 확인해주세요.";
            fields=validation.getBindingResult().getFieldErrors().stream()
                    .map(e->new ApiErrorResponse.FieldError(e.getField(),validationMessage(e.getCode())))
                    .distinct().sorted(Comparator.comparing(ApiErrorResponse.FieldError::field)).toList();
        }else if(exception instanceof HttpMessageNotReadableException){code="INVALID_JSON";message="JSON 요청 본문과 값의 형식을 확인해주세요.";}
        else if(exception instanceof MissingServletRequestParameterException missing){
            code="MISSING_PARAMETER";message="필수 요청 값이 누락되었습니다.";
            fields=List.of(new ApiErrorResponse.FieldError(missing.getParameterName(),"필수 값입니다."));
        }else if(exception instanceof TypeMismatchException && status<500){
            code="INVALID_PARAMETER";message="요청 값의 형식을 확인해주세요.";
            if(exception instanceof MethodArgumentTypeMismatchException mismatch)
                fields=List.of(new ApiErrorResponse.FieldError(mismatch.getName(),"값의 형식이 올바르지 않습니다."));
        }else if(exception instanceof HandlerMethodValidationException && status<500){code="VALIDATION_FAILED";message="입력값을 확인해주세요.";}
        else if(exception instanceof jakarta.validation.ConstraintViolationException){code="VALIDATION_FAILED";message="입력값을 확인해주세요.";}
        else if(exception instanceof org.springframework.dao.OptimisticLockingFailureException
                || exception instanceof org.springframework.dao.PessimisticLockingFailureException){code="CONCURRENT_MODIFICATION";message="다른 요청이 처리 중입니다. 최신 상태를 조회한 뒤 다시 시도해주세요.";}
        else if(exception instanceof org.springframework.dao.DataIntegrityViolationException){code="DATA_CONFLICT";message="이미 존재하거나 연결된 데이터와 충돌합니다.";}
        else if(exception instanceof ResponseStatusException reason && status<500 && reason.getReason()!=null){message=reason.getReason();}
        if(status>=500){message=message(status);org.slf4j.LoggerFactory.getLogger(getClass()).error("API failure requestId={} status={} type={}",
                ApiRequestIdFilter.requestId(request),status,exception==null?"ServletError":exception.getClass().getSimpleName());}
        String path=request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) instanceof String uri?uri:request.getRequestURI();
        // Do not reflect query parameters, rejected values, SQL, exception messages, or tokens.
        var body=new ApiErrorResponse(new ApiErrorResponse.Detail(code,message,status,path,Instant.now(),ApiRequestIdFilter.requestId(request),fields));
        var headers=new HttpHeaders();headers.putAll(original);
        headers.setContentType(MediaType.APPLICATION_JSON);headers.setCacheControl("no-store");
        headers.set("X-Request-Id",body.error().requestId());
        if(status==401)headers.set(HttpHeaders.WWW_AUTHENTICATE,
                "DEVICE_AUTH_FAILED".equals(code) ? "DeviceKey realm=\"nyangttang\"" : "Bearer");
        return new ResponseEntity<>(body,headers,HttpStatusCode.valueOf(status));
    }
    public static String code(int status){return switch(status){
        case 400->"BAD_REQUEST";case 401->"UNAUTHORIZED";case 403->"FORBIDDEN";case 404->"NOT_FOUND";
        case 405->"METHOD_NOT_ALLOWED";case 406->"NOT_ACCEPTABLE";case 409->"CONFLICT";case 413->"PAYLOAD_TOO_LARGE";
        case 415->"UNSUPPORTED_MEDIA_TYPE";case 429->"TOO_MANY_REQUESTS";case 503->"SERVICE_UNAVAILABLE";default->"INTERNAL_SERVER_ERROR";};}
    private static String message(int status){return switch(status){
        case 400->"요청 내용을 확인해주세요.";case 401->"유효한 로그인 세션이 필요합니다.";case 403->"접근 권한이 없습니다.";
        case 404->"요청한 대상을 찾을 수 없습니다.";case 405->"지원하지 않는 HTTP 메서드입니다.";
        case 406->"지원하지 않는 응답 형식입니다.";case 409->"현재 상태에서는 요청을 처리할 수 없습니다.";
        case 413->"요청 크기가 너무 큽니다.";case 415->"Content-Type을 확인해주세요.";
        case 429->"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";case 503->"서비스를 일시적으로 사용할 수 없습니다.";
        default->"서버에서 요청을 처리하지 못했습니다.";};}
    private static String validationMessage(String code){
        if(code==null)return "입력값을 확인해주세요.";
        return switch(code){case "NotNull","NotBlank","NotEmpty"->"필수 값입니다.";
            case "Size","Length"->"허용된 길이를 확인해주세요.";
            case "Min","Max","Positive","PositiveOrZero","DecimalMin","DecimalMax"->"허용된 숫자 범위를 확인해주세요.";
            case "Pattern","Email"->"입력 형식을 확인해주세요.";default->"입력값을 확인해주세요.";};
    }
}
