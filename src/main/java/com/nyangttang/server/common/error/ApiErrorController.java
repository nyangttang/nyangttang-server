package com.nyangttang.server.common.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ApiErrorController implements ErrorController {
    private final ApiErrors errors;
    public ApiErrorController(ApiErrors errors){this.errors=errors;}
    @RequestMapping("${server.error.path:${error.path:/error}}")
    public ResponseEntity<Object> error(HttpServletRequest request){
        int status=request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) instanceof Integer code?code:404;
        if(status<400 || status>599)status=500;
        return errors.response(null,status,new HttpHeaders(),request);
    }
}
