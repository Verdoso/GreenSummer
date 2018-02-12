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
import java.util.List;

import org.greeneyed.summer.config.JoltViewConfiguration;
import org.greeneyed.summer.config.Log4jMDCFilterConfiguration;
import org.greeneyed.summer.config.MessageSourceConfiguration;
import org.greeneyed.summer.config.SummerWebConfig;
import org.greeneyed.summer.config.XsltConfiguration;
import org.greeneyed.summer.controller.HealthController;
import org.greeneyed.summer.controller.Log4JController;
import org.greeneyed.summer.util.ActuatorCustomizer;
import org.greeneyed.summer.util.ApplicationContextProvider;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

public class EnableSummerImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        AnnotationAttributes attributes =
            AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableSummer.class.getName(), false));
        List<String> configurationClassesToEnable = new ArrayList<>();
        if (attributes.getBoolean("message_source")) {
            configurationClassesToEnable.add(MessageSourceConfiguration.class.getName());
        }
        if (attributes.getBoolean("log4j")) {
            configurationClassesToEnable.add(Log4JController.class.getName());
        }
        if (attributes.getBoolean("log4j_filter")) {
            configurationClassesToEnable.add(Log4jMDCFilterConfiguration.class.getName());
        }
        if (attributes.getBoolean("health")) {
            configurationClassesToEnable.add(HealthController.class.getName());
        }
        if (attributes.getBoolean("actuator_customizer")) {
            configurationClassesToEnable.add(ActuatorCustomizer.class.getName());
        }
        if (attributes.getBoolean("xslt_view")) {
            configurationClassesToEnable.add(XsltConfiguration.class.getName());
        }
        if (attributes.getBoolean("jolt_view")) {
            configurationClassesToEnable.add(ApplicationContextProvider.class.getName());
            configurationClassesToEnable.add(JoltViewConfiguration.class.getName());
        }
        if (attributes.getBoolean("xml_view_pooling")) {
            configurationClassesToEnable.add(SummerWebConfig.class.getName());
        }
        return configurationClassesToEnable.toArray(new String[configurationClassesToEnable.size()]);
    }
}
