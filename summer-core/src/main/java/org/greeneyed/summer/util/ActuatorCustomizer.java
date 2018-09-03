package org.greeneyed.summer.util;

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


import java.util.LinkedHashSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.endpoint.mvc.ActuatorMediaTypes;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * An EndpointHandlerMappingCustomizer that changes the order of the media types returned from the actuator so the media types with the version number
 * included are at the end (so regular clients recognize the response a JSON)
 *
 */
@Component
public class ActuatorCustomizer implements EndpointHandlerMappingCustomizer {

    static class Fix extends HandlerInterceptorAdapter {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            Object attribute = request.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
            if (attribute instanceof LinkedHashSet) {
                @SuppressWarnings("unchecked") LinkedHashSet<MediaType> lhs = (LinkedHashSet<MediaType>) attribute;
                if (lhs.remove(ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON)) {
                    lhs.add(ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON);
                }
            }
            return true;
        }
    }

    @Override
    public void customize(EndpointHandlerMapping mapping) {
        mapping.setInterceptors(new Fix());
    }
}
