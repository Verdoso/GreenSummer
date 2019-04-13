package org.greeneyed.summer.config;

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


import java.util.List;

import org.greeneyed.summer.util.jaxb.CustomXMLHttpMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Data
@EqualsAndHashCode(callSuper = false)
@ConfigurationProperties(prefix = "summer")
@Slf4j
public class SummerWebConfig extends WebMvcConfigurerAdapter {

    @Value("${summer.xml_http.poolsMaxPerKey:10}")
    private int poolsMaxPerKey;

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        log.debug("Configuring XMLHTTPMessageConverter with a pool of {} per key", poolsMaxPerKey);
        converters.add(new CustomXMLHttpMessageConverter(poolsMaxPerKey));
        super.configureMessageConverters(converters);
    }
}
