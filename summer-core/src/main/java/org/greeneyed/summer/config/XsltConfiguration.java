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


import java.util.Locale;

import org.greeneyed.summer.util.SummerXSLTView;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.xslt.XsltViewResolver;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@ConfigurationProperties(prefix = "summer.xslt")
@Slf4j
@Data
public class XsltConfiguration {

    public static final String DEFAULT_PREFFIX = "classpath:/xslt/";
    public static final String XSLT_SUFFIX = ".xslt";
    public static final String XML_SOURCE_TAG = "xmlSource";
    public static final String REFRESH_XSLT_FLAG = "refreshXSLT";
    public static final String SHOW_XML_SOURCE_FLAG = "showXMLSource";
    public static final int TOTAL_FACTOR = 5;
    public static final double MIN_IDLE_FACTOR = 0.5;
    public static final double MAX_IDLE_FACTOR = 0.75;
    private String preffix = DEFAULT_PREFFIX;
    private boolean devMode = false;
    private int poolsMaxPerKey = 5;
    private MediaType mediaType;

    public static class XsltModelAndView extends ModelAndView {

        public XsltModelAndView(String viewName, Object modelObject) {
            super(viewName + XSLT_SUFFIX, XML_SOURCE_TAG, modelObject);
        }

        public XsltModelAndView(String viewName, Object modelObject, HttpStatus status) {
            super(viewName + XSLT_SUFFIX, status);
            getModelMap().put(XML_SOURCE_TAG, modelObject);
        }
    }

    public static class CustomXsltViewResolver extends XsltViewResolver {
        @Override
        protected View loadView(String viewName, Locale locale) throws Exception {
            View view = super.loadView(viewName, locale);
            if (view instanceof MessageSourceAware) {
                ((MessageSourceAware) view).setMessageSource(getApplicationContext());
            }
            return view;
        }
    }

    @Bean
    public ViewResolver getXSLTViewResolver() {
        log.debug("Configuring SummerXSLTView");
        XsltViewResolver xsltResolver = new CustomXsltViewResolver();
        xsltResolver.setOrder(1);
        xsltResolver.setSourceKey(XML_SOURCE_TAG);
        xsltResolver.setViewClass(SummerXSLTView.class);
        xsltResolver.setViewNames(new String[] {"*" + XSLT_SUFFIX});
        xsltResolver.setPrefix(preffix);
        return xsltResolver;
    }
}
