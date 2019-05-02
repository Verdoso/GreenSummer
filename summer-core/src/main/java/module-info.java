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
    
    requires transitive java.activation;
    requires transitive java.annotation;
    requires transitive com.fasterxml.jackson.core;
    requires transitive com.fasterxml.jackson.databind;
    requires transitive org.slf4j;
    requires transitive jaxb.core;
    requires transitive javax.servlet.api;
    requires transitive commons.pool2;
    requires transitive spring.context;
    requires transitive spring.core;
    requires transitive spring.web;
    requires transitive spring.boot;
    requires transitive spring.boot.actuator;
    requires transitive spring.boot.autoconfigure;
    requires transitive spring.context.support;
    requires transitive spring.beans;
    requires transitive spring.cloud.context;
    requires transitive org.apache.logging.log4j.core;
    requires transitive org.apache.logging.log4j;
    //requires json.utils;
    //requires static jolt.core;
    
    requires transitive java.xml.bind;
    requires transitive spring.webmvc;
    
    requires commons.logging;
    requires static transitive ch.qos.logback.classic;
    requires static transitive ch.qos.logback.core;
    requires static lombok;
    requires transitive hazelcast.all;
    requires transitive cqengine;
    requires hazelcast.consul.discovery.spi;
    requires transitive com.github.benmanes.caffeine;
    requires transitive org.aspectj.weaver;
}
