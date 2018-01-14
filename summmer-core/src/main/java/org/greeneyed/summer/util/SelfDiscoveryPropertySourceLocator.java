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


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelfDiscoveryPropertySourceLocator implements PropertySourceLocator {

    @Override
    public PropertySource<?> locate(Environment environment) {
        MapPropertySource result = new MapPropertySource("SelfDiscoveredProperty", Collections.emptyMap());
        try {
            String localhostName = InetAddress.getLocalHost().getCanonicalHostName();
            if (localhostName != null) {
                String hostname = localhostName.toLowerCase();
                log.info("Setting hostname to {}", hostname);
                result = new MapPropertySource("SelfDiscoveredProperty",
                    Collections.<String, Object>singletonMap("spring.cloud.consul.discovery.hostname", hostname));
            }
        } catch (UnknownHostException e) {
            log.error("Error obtaining localhost name", e);
        }
        return result;
    }

}
