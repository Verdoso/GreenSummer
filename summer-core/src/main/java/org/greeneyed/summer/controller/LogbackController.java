package org.greeneyed.summer.controller;

import java.io.File;

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
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.greeneyed.summer.messages.LogResponse;
import org.greeneyed.summer.messages.LogSpecification;
import org.greeneyed.summer.util.logging.LogbackMemoryAppenderImpl;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class Log4JController.
 * 
 * Path can be configured through the properties. For example:
 * 
 * <pre>
 * summer:
 *  logback:
 *    path: /secret/logback
 * </pre>
 * 
 * Pattern can also be configured through the properties (unfortunately the global one cannot be retrieved from Logback)
 * . For example (the default):
 * 
 * <pre>
 * summer:
 *  logback:
 *    in_memory_pattern: "%-5p|%date{ISO8601}|%X{Slf4jMDCFilter.UUID}|%logger{0}|%m%ex%n"
 * </pre>
 */
@Data
@Controller
@RequestMapping({"${summer.logback.path:/logback}"})
@Slf4j
@ConfigurationProperties(prefix = "summer.logback")
public class LogbackController {

    private String inMemoryPattern = "%-5p|%date{ISO8601}|%X{Slf4jMDCFilter.UUID}|%logger{0}|%m%ex%n";

    private LogbackMemoryAppenderImpl logbackMemoryAppenderImpl;

    /**
     * Setup.
     */
    @PostConstruct
    public void setup() {
        initInMemoryAppender();
    }

    private void initInMemoryAppender() {
        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        synchronized (ctx) {
            logbackMemoryAppenderImpl = new LogbackMemoryAppenderImpl(200, inMemoryPattern);
            logbackMemoryAppenderImpl.setName("LogbackMemoryAppender");
            logbackMemoryAppenderImpl.setContext(ctx);
            logbackMemoryAppenderImpl.start();
        }
    }

    /**
     * 
     * @return
     */
    @RequestMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> list() {
        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    /**
     * 
     * @param name
     * @param level
     * @return
     */
    @RequestMapping(value = "set/{name}/{level}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET,
        headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> set(@PathVariable("name")
    final String name, @PathVariable("level")
    final String level) {
        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        synchronized (ctx) {
            Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
            Logger logger = ctx.getLogger(name);
            if (logger != null) {
                logger.setLevel(Level.toLevel(level));
                logger.setAdditive(false);
                for (Iterator<Appender<ILoggingEvent>> it = root.iteratorForAppenders(); it.hasNext();) {
                    logger.addAppender(it.next());
                }
            }
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    /**
     * 
     * @param name
     * @return
     */
    @RequestMapping(value = "unset/{name}/", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET,
        headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> unset(@PathVariable("name")
    final String name) {
        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        synchronized (ctx) {
            Logger logger = ctx.getLogger(name);
            if (logger != null) {
                logger.setLevel(null);
            }
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    @RequestMapping(value = "reset", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> reset() {
        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        synchronized (ctx) {
            File configuration = ConfigurationWatchListUtil.getConfigurationWatchList(ctx).getCopyOfFileWatchList().get(0);
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(ctx);
            ctx.reset();
            try {
                configurator.doConfigure(configuration);
            } catch (JoranException e) {
                log.error("Error re-applying the configuration");
            }
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    private LogResponse listLoggers(final LoggerContext ctx) {
        LogResponse logResponse = new LogResponse();
        List<LogSpecification> result = new ArrayList<>();
        logResponse.setSpecs(result);
        synchronized (ctx) {
            ctx.getLoggerList().stream().filter(logger -> logger.getLevel() != null).forEach(logger -> {
                List<String> appenders = new ArrayList<>();
                for (Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders(); it.hasNext();) {
                    appenders.add(it.next().getName());
                }
                result.add(new LogSpecification(logger.getName(), logger.getEffectiveLevel().toString(), appenders));
            });
        }
        return logResponse;

    }

    /**
     * Captures the given logger at the given level so it can be displayed directly
     * by this controller.
     * 
     * @param name
     * @param level
     * @param append
     * @return
     */
    @RequestMapping(value = "capture/{name}/{level}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET,
        headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> capture(@PathVariable("name")
    final String name, @PathVariable("level")
    final String level, @RequestParam(value = "append", defaultValue = "false") boolean append) {
        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        synchronized (ctx) {
            if (logbackMemoryAppenderImpl != null) {
                Logger logger = ctx.getLogger(name);
                if (logger != null) {
                    logger.setLevel(Level.toLevel(level));
                    logger.setAdditive(append);
                    logger.addAppender(logbackMemoryAppenderImpl);
                }
            }
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    /**
     * Frees the given logger from the appender used to be displayed directly by
     * this controller.
     *
     * @param name
     * @return response
     */
    @RequestMapping(value = "free/{name}/", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET,
        headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> free(@PathVariable("name")
    final String name) {
        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        synchronized (ctx) {
            Logger logger = ctx.getLogger(name);
            if (logger != null) {
                logger.setLevel(null);
                logger.detachAppender(logbackMemoryAppenderImpl.getName());
            }
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    /**
     * 
     * @param response
     */
    @RequestMapping(value = "captured", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, headers = "Accept=application/json")
    public void captured(HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        synchronized (ctx) {
            if (logbackMemoryAppenderImpl != null) {
                logbackMemoryAppenderImpl.getRegisteredEvents().stream().forEach(line -> {
                    try {
                        response.getWriter().write(line);
                    } catch (Exception e) {
                        log.error("Error showing captured logs. Ironic, huh?", e);
                    }
                });
            }
        }
    }

}
