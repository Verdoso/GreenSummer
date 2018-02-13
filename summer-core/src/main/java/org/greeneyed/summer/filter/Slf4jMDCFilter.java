package org.greeneyed.summer.filter;

/*
 * #%L
 * Summer
 * %%
 * Copyright (C) 2018 GreenEyed (Daniel Lopez)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import java.util.UUID;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Component(value = "Log4UUIDFilter")
public class Log4jMDCFilter extends OncePerRequestFilter {

    public static final String RESPONSE_TOKEN_HEADER = "Response_Token";
    public static final String MDC_UUID_TOKEN_KEY = "Log4UUIDFilter.UUID";

    /**
     * Instantiates a new log 4 j MDC filter.
     */
    public Log4jMDCFilter() {
        // Default constructor as per spec.
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
        throws java.io.IOException, ServletException {
        try {
            String token = UUID.randomUUID().toString().toUpperCase().replace("-", "");
            MDC.put(MDC_UUID_TOKEN_KEY, token);
            response.addHeader(RESPONSE_TOKEN_HEADER, token);
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_UUID_TOKEN_KEY);
        }
    }

    @Override
    protected boolean isAsyncDispatch(final HttpServletRequest request) {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }
}
