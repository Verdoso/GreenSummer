/*-
 * #%L
 * GreenSummer
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
open module org.greeneyed.summer {
    exports org.greeneyed.summer.config.enablers;
    exports org.greeneyed.summer.util.jaxb;
    exports org.greeneyed.summer.util.autoformatter;
    exports org.greeneyed.summer.util.logging;
    exports org.greeneyed.summer.filter;
    exports org.greeneyed.summer.messages;
    exports org.greeneyed.summer.controller;
    exports org.greeneyed.summer.monitoring;
    exports org.greeneyed.summer.util;
    exports org.greeneyed.summer.config;
    
    requires java.xml;
    requires java.xml.ws.annotation;
    requires javax.inject;
    requires jaxb.core;
    requires javax.servlet.api;
    requires commons.pool2;
    requires spring.context;
    requires spring.core;
    requires spring.web;
    requires spring.boot;
    requires spring.boot.actuator;
    requires spring.boot.autoconfigure;
    requires spring.context.support;
    requires spring.beans;
    requires spring.cloud.context;
    requires slf4j.api;
    requires jackson.annotations;
    requires jackson.core;
    requires jackson.databind;
    requires aspectjweaver;
    requires org.apache.logging.log4j;
    //requires json.utils;
    
    requires transitive java.xml.bind;
    requires transitive spring.webmvc;
    
    requires static commons.logging;
//    requires static log4j.core;
    requires static caffeine;
    requires static jolt.core;
    requires static ch.qos.logback.classic;
    requires static ch.qos.logback.core;
    requires static lombok;
}
