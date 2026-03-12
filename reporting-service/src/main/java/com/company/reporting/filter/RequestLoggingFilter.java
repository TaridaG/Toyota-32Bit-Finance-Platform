package com.company.reporting.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class RequestLoggingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) request;

        long start = System.currentTimeMillis();

        chain.doFilter(request, response);

        long took = System.currentTimeMillis() - start;

        log.info("HTTP {} {} took={}ms",
                http.getMethod(),
                http.getRequestURI(),
                took);
    }
}