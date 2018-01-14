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

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.Data;

/**
 * The Class HealthController.
 */
@Data
@Controller
@RequestMapping({
    "/health"})
public class HealthController {

    public static enum STATUS {
        OK,
        DISABLED
    }

    private STATUS status = STATUS.OK;

    /**
     * @return The status
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> checkHealth() {
        return currentStatus();
    }

    /**
     * Enable.
     *
     * @return the response entity
     */
    @RequestMapping(value = "/enable", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> enable() {
        status = STATUS.OK;
        return currentStatus();
    }

    /**
     * Disable.
     *
     * @return the response entity
     */
    @RequestMapping(value = "/disable", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, String>> disable() {
        status = STATUS.DISABLED;
        return currentStatus();
    }

    private ResponseEntity<Map<String, String>> currentStatus() {
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("status", status.name());
        return new ResponseEntity<>(resultMap, HttpStatus.OK);
    }

}
