package com.nyangttang.server.common.error;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

@Component @Order(Ordered.HIGHEST_PRECEDENCE+20)
public class ApiRequestIdFilter extends OncePerRequestFilter {
    public static final String ATTRIBUTE=ApiRequestIdFilter.class.getName()+".id";
    public static String requestId(HttpServletRequest request){
        if(request.getAttribute(ATTRIBUTE) instanceof String existing)return existing;
        String id=UUID.randomUUID().toString();request.setAttribute(ATTRIBUTE,id);return id;
    }
    @Override protected boolean shouldNotFilterErrorDispatch(){return false;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        res.setHeader("X-Request-Id",requestId(req));chain.doFilter(req,res);
    }
}
