package org.greeneyed.summer.controller;

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


import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.Data;

/**
 * The Class HealthController.
 * 
 * Path can be configured through the properties ((default is /health). For example:
 * 
 * <pre>
 * summer:
 *   health:
 *     path: /secret/health
 * </pre>
 * 
 * Starting status can also be configured, in case you want the application to start as disabled(KO) (default is ok)
 * 
 * <pre>
 * summer:
 *   health:
 *     status: KO
 * </pre>
 * 
 * You can also tell the controller to return a HTTP - 503 code instead of a 200 to when it is KO (default is false)
 * 
 * <pre>
 * summer:
 *   health:
 *     use_http_status: true
 * </pre>
 */
@Data
@Controller
@RequestMapping({"${summer.health.path:/health}"})
@ConfigurationProperties(prefix = "summer.health")
public class HealthController {

    public static enum STATUS {
        OK,
        KO
    }

    private STATUS status = STATUS.OK;
    private boolean useHttpStatus = false;

    @RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> status(HttpServletRequest request) {
        return currentStatus(request);
    }

    @RequestMapping(value = "/enable", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> enable(HttpServletRequest request) {
        status = STATUS.OK;
        return currentStatus(request);
    }

    @RequestMapping(value = "/disable", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> disable(HttpServletRequest request) {
        status = STATUS.KO;
        return currentStatus(request);
    }

    private ResponseEntity<Map<String, String>> currentStatus(HttpServletRequest request) {
        Map<String, String> resultMap = new HashMap<>();
        STATUS currentStatus = status;
        resultMap.put("status", currentStatus.name());
        Map<String, String> customStatus = getCustomStatus(request);
        if (customStatus != null) {
            resultMap.putAll(customStatus);
        }
        final HttpStatus httpStatus = (currentStatus == STATUS.KO && useHttpStatus) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return new ResponseEntity<>(resultMap, httpStatus);
    }

    /**
     * Method to override if you want to add custom status messages to the default OK/KO
     * 
     * @param request
     * @return
     */
    protected Map<String, String> getCustomStatus(HttpServletRequest request) {
        return null;
    }

}
