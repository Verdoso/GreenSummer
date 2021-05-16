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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MessageSourceConfiguration {
    private static final String CLASSPATH_PREFIX = "classpath:";

    @Value("${labels.fileName:labels}")
    private String fileName;

    @Value("${labels.languageParameter:language}")
    private String languageParameter;

    @Value("${labels.defaultLocale:}")
    private String defaultLocale;

    @Value("${labels.reload:false}")
    private boolean reloadLabels;

    @Value("${labels.use_code_as_default:false}")
    private boolean useCodeAsDefault;

    @Bean
    public SessionLocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        if (StringUtils.hasText(defaultLocale)) {
            localeResolver.setDefaultLocale(new Locale(defaultLocale));
            log.info("Labels default language set to {} ", defaultLocale);
        }
        return localeResolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName(languageParameter);
        return localeChangeInterceptor;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename(CLASSPATH_PREFIX + fileName);
        // if true, the key of the message will be displayed if the key is not
        // found, instead of throwing a NoSuchMessageException
        messageSource.setUseCodeAsDefaultMessage(useCodeAsDefault);
        messageSource.setDefaultEncoding("UTF-8");
        // // # -1 : never reload, 0 always reload
        log.info("Labels {} be reloaded", reloadLabels ? "will" : "won't");
        messageSource.setCacheSeconds(reloadLabels ? 0 : -1);
        return messageSource;
    }
}
