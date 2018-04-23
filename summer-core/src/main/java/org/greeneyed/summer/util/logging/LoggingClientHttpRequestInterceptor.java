package org.greeneyed.summer.util.logging;

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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

import org.greeneyed.summer.util.ObjectJoiner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Allows logging outgoing requests and the corresponding responses. Requires
 * the use of a {@link org.springframework.http.client.BufferingClientHttpRequestFactory} to
 * log the body of received responses.
 */
@Slf4j
public class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";
    public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
    public static final String APPLICATION_JSON_HEADER = "application/json";

    private volatile boolean loggedMissingBuffering;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (log.isInfoEnabled()) {
            logRequest(request, body);
        }
        ClientHttpResponse response = execution.execute(request, body);
        if (log.isInfoEnabled()) {
            logResponse(request, response);
        }
        return response;
    }

    protected void logRequest(HttpRequest request, byte[] body) {
        log.info("Request: {}", ObjectJoiner.join(" ", request.getURI().getScheme().toUpperCase(), request.getMethod(), request.getURI()));
        final boolean hasRequestBody = body != null && body.length > 0;
        if (log.isDebugEnabled()) {
            // If the request has a body, sometimes these headers are not
            // present, so let's make them explicit
            if (hasRequestBody) {
                logHeader(CONTENT_LENGTH_HEADER, Long.toString(body.length));
                final MediaType contentType = request.getHeaders().getContentType();
                if (contentType != null) {
                    logHeader(CONTENT_TYPE_HEADER, contentType.toString());
                }
            }
            // Log the other headers
            for (String header : request.getHeaders().keySet()) {
                if (!CONTENT_TYPE_HEADER.equalsIgnoreCase(header) && !CONTENT_LENGTH_HEADER.equalsIgnoreCase(header)) {
                    for (String value : request.getHeaders().get(header)) {
                        logHeader(header, value);
                    }
                }
            }
            if (log.isTraceEnabled() && hasRequestBody) {
                logBody(new String(body, determineCharset(request.getHeaders())), request.getHeaders());
            }
        }
    }

    protected void logResponse(HttpRequest request, ClientHttpResponse response) {
        try {
            log.info("Response: {}",
                ObjectJoiner.join(" ", response.getRawStatusCode(), response.getStatusText(), " from ", request.getMethod(), ": ", request.getURI()));
            if (log.isDebugEnabled()) {
                HttpHeaders responseHeaders = response.getHeaders();
                for (String header : response.getHeaders().keySet()) {
                    for (String value : response.getHeaders().get(header)) {
                        logHeader(header, value);
                    }
                }
                if (log.isTraceEnabled() && hasTextBody(responseHeaders) && isBuffered(response)) {
                    logBody(StreamUtils.copyToString(response.getBody(), determineCharset(responseHeaders)), responseHeaders);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to log response for {} request to {}", request.getMethod(), request.getURI(), e);
        }
    }

    private void logHeader(final String headerName, final String headerValue) {
        log.debug("  Header: {}: \"{}\"", headerName, headerValue);
    }

    private void logBody(String body, HttpHeaders headers) {
        MediaType contentType = headers.getContentType();
        List<String> contentEncoding = headers.get(CONTENT_ENCODING_HEADER);
        if (contentEncoding != null && !contentEncoding.contains("identity")) {
            log.trace("  Body: encoded, not shown");
        } else {
            if (contentType != null && contentType.toString().startsWith(APPLICATION_JSON_HEADER)) {
                log.trace("  Body: {}", writeJSON(body));
            } else {
                log.trace("  Body: {}", body);
            }
        }
    }

    private static String writeJSON(final Object object) {
        ObjectMapper mapper = null;
        String result = null;
        mapper = new ObjectMapper();
        try {
            if (object instanceof String) {
                Object json = mapper.readValue((String) object, Object.class);
                result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            } else {
                result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            }
        } catch (final IOException e) {
            log.warn("Body is not a json object {}", e.getMessage());
        }
        return result;
    }

    private Charset determineCharset(HttpHeaders headers) {
        Charset resultCharset = StandardCharsets.UTF_8;
        MediaType contentType = headers.getContentType();
        if (contentType != null) {
            try {
                Charset charSet = contentType.getCharset();
                if (charSet != null) {
                    resultCharset = charSet;
                }
            } catch (UnsupportedCharsetException e) {
                log.error("Error setting charset", e);
            }
        }
        return resultCharset;
    }

    private boolean hasTextBody(HttpHeaders headers) {
        long contentLength = headers.getContentLength();
        if (contentLength != 0) {
            MediaType contentType = headers.getContentType();
            if (contentType != null) {
                String subtype = contentType.getSubtype();
                return "text".equals(contentType.getType()) || "xml".equals(subtype) || "json".equals(subtype);
            }
        }
        return false;
    }

    private boolean isBuffered(ClientHttpResponse response) {
        // class is non-public, so we check by name
        boolean buffered = "org.springframework.http.client.BufferingClientHttpResponseWrapper".equals(response.getClass().getName());
        if (!buffered && !loggedMissingBuffering) {
            log.warn("Can't log HTTP response bodies, as you haven't configured the RestTemplate with a BufferingClientHttpRequestFactory");
            loggedMissingBuffering = true;
        }
        return buffered;
    }

}
