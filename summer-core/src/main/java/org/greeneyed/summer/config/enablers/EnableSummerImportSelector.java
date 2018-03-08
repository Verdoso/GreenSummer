package org.greeneyed.summer.config.enablers;

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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.greeneyed.summer.config.JoltViewConfiguration;
import org.greeneyed.summer.config.MessageSourceConfiguration;
import org.greeneyed.summer.config.Slf4jMDCFilterConfiguration;
import org.greeneyed.summer.config.SummerWebConfig;
import org.greeneyed.summer.config.XsltConfiguration;
import org.greeneyed.summer.controller.HealthController;
import org.greeneyed.summer.controller.Log4JController;
import org.greeneyed.summer.controller.LogbackController;
import org.greeneyed.summer.util.ActuatorCustomizer;
import org.greeneyed.summer.util.ApplicationContextProvider;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnableSummerImportSelector implements ImportSelector {

    private static enum ENABLE_OPTION {
        MESSAGE_SOURCE("message_source", MessageSourceConfiguration.class),
        LOG4J_CONTROLLER(
            "log4j",
            new Class[] {
                Log4JController.class},
            new String[] {
                "org.apache.logging.log4j.core.LoggerContext"}),
        LOGBACK_CONTROLLER(
            "logback",
            new Class[] {
                LogbackController.class},
            new String[] {
                "ch.qos.logback.classic.LoggerContext"}),
        SLF4J_FILTER(
            "slf4j_filter",
            new Class[] {
                Slf4jMDCFilterConfiguration.class},
            new String[] {
                "org.slf4j.MDC"}),
        HEALTH_CONTROLLER("health", HealthController.class),
        ACTUATOR_CUSTOMIZER("actuator_customizer", ActuatorCustomizer.class),
        XSLT_VIEW("xslt_view", XsltConfiguration.class),
        JOLT_VIEW(
            "jolt_view",
            new Class[] {
                ApplicationContextProvider.class,
                JoltViewConfiguration.class},
            new String[] {
                "com.bazaarvoice.jolt.Chainr"}),
        XML_VIEW_POOLING("xml_view_pooling", SummerWebConfig.class),
        CAFFEINE_CACHE(
            "caffeine_cache",
            new Class[] {
                SummerWebConfig.class},
            new String[] {
                "org.springframework.cache.caffeine.CaffeineCache",
                "com.github.benmanes.caffeine.cache.Caffeine"});

        private final String flag;
        private final Class<?>[] configurationClasses;
        private final String[] requirementClasses;

        private ENABLE_OPTION(final String flag, final Class<?>[] configurationClasses, final String[] requirementClasses) {
            this.flag = flag;
            this.configurationClasses = configurationClasses;
            this.requirementClasses = requirementClasses;
        }

        private ENABLE_OPTION(final String flag, final Class<?> configurationClass) {
            this(flag, new Class[] {
                configurationClass}, null);
        }
    }


    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        AnnotationAttributes attributes =
            AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableSummer.class.getName(), false));
        List<String> configurationClassesToEnable = new ArrayList<>();
        for (ENABLE_OPTION option : ENABLE_OPTION.values()) {
            if (attributes.getBoolean(option.flag)) {
                try {
                    if (option.requirementClasses != null) {
                        for (String requiredClass : option.requirementClasses) {
                            Class.forName(requiredClass);
                        }
                    }
                    for (Class<?> configuredClass : option.configurationClasses) {
                        configurationClassesToEnable.add(configuredClass.getName());
                    }
                } catch (Exception e) {
                    log.error("Error enabling module: {}. It requires classes {} in the classpath. {}:{}", option.flag,
                        Arrays.stream(option.requirementClasses).collect(Collectors.joining(", ")), e.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
        return configurationClassesToEnable.toArray(new String[configurationClassesToEnable.size()]);
    }
}
