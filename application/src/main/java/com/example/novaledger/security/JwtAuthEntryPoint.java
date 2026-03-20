package com.example.novaledger.security;

import com.example.novaledger.common.exception.ErrorCode;
import com.example.novaledger.common.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest req,
                         HttpServletResponse res,
                         AuthenticationException ex)
            throws IOException {

        ApiErrorResponse error = new ApiErrorResponse(
                (String) req.getAttribute("traceId"),
                ErrorCode.JWT_INVALID.getCode(),
                401,
                ErrorCode.JWT_INVALID.getMessage(),
                req.getRequestURI()
        );

        res.setStatus(401);
        res.setContentType("application/json");
        new ObjectMapper().writeValue(res.getOutputStream(), error);
    }
}
