package org.greeneyed.summer.controller;

import java.nio.file.Paths;

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
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.greeneyed.summer.messages.LogResponse;
import org.greeneyed.summer.messages.LogSpecification;
import org.greeneyed.summer.util.InMemoryAppenderImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class Log4JController.
 * 
 * Path can be configured through the properties. For example:
 * summer:
 * log4j:
 * path: /secret/log4j
 */
@Data
@Controller
@RequestMapping({"${summer.log4j.path:/log4j}"})
@Slf4j
public class Log4JController {

    private InMemoryAppenderImpl inMemoryAppenderImpl;

    /**
     * Setup.
     */
    @PostConstruct
    public void setup() {
        initInMemoryAppender();
    }

    private void initInMemoryAppender() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        synchronized (ctx) {
            inMemoryAppenderImpl = null;
            final Configuration config = ctx.getConfiguration();
            Optional<Appender> baseAppender = ctx.getConfiguration().getAppenders().values().stream().findFirst();

            if (baseAppender.isPresent()) {
                inMemoryAppenderImpl =
                    InMemoryAppenderImpl.createAppender("InMemoryAppenderImplAppender", baseAppender.get().getLayout(), config.getFilter(), 200);
                inMemoryAppenderImpl.start();
                config.addAppender(inMemoryAppenderImpl);
            }
        }
    }

    /**
     * List.
     *
     * @return the response entity
     */
    @RequestMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> list() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    /**
     * Sets the.
     *
     * @param name
     *        the name
     * @param level
     *        the level
     * @return the response entity
     */
    @RequestMapping(value = "set/{name}/{level}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET,
        headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> set(@PathVariable("name")
    final String name, @PathVariable("level")
    final Level level) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        synchronized (ctx) {
            final Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(name);
            if (name.equalsIgnoreCase(loggerConfig.getName())) {
                loggerConfig.setLevel(level);
            } else {
                LoggerConfig newloggerConfig = new LoggerConfig(name, level, true);
                config.addLogger(name, newloggerConfig);
            }
            ctx.updateLoggers();
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    /**
     * Unset.
     *
     * @param name
     *        the name
     * @return the response entity
     */
    @RequestMapping(value = "unset/{name}/", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET,
        headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> unset(@PathVariable("name")
    final String name) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        synchronized (ctx) {
            final Configuration config = ctx.getConfiguration();
            config.removeLogger(name);
            ctx.updateLoggers();
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    /**
     * Reset.
     *
     * @return the response entity
     */
    @RequestMapping(value = "reset", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> reset() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        synchronized (ctx) {
            // If the config location is null, it means we are using a non-standard path for 
            // the config file (for example log4j2-spring.xml. In this case the name of the configuration
            // is usually the path to the configuration file. so we "fix" the path before reconfiguring
            if (ctx.getConfigLocation() == null) {
                ctx.setConfigLocation(Paths.get(ctx.getConfiguration().getName()).toUri());
            }
            ctx.reconfigure();
            initInMemoryAppender();
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    private LogResponse listLoggers(final LoggerContext ctx) {
        LogResponse logResponse = new LogResponse();
        List<LogSpecification> result = new ArrayList<>();
        logResponse.setSpecs(result);
        synchronized (ctx) {
            final Configuration config = ctx.getConfiguration();
            config.getLoggers().forEach(
                (name, configuration) -> result.add(new LogSpecification(StringUtils.hasText(name) ? name : "Root", configuration.getLevel().name(),
                    configuration.getAppenderRefs().stream().map(AppenderRef::getRef).collect(Collectors.toList()))));
        }
        return logResponse;

    }

    /**
     * Captures the given logger at the given level so it can be displayed directly by this controller.
     *
     * @param name the name
     * @param level the level
     * @param append the append
     * @return the response entity
     */
    @RequestMapping(value = "capture/{name}/{level}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET,
        headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> capture(@PathVariable("name")
    final String name, @PathVariable("level")
    final Level level, @RequestParam(value = "append", defaultValue = "false") boolean append) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        synchronized (ctx) {
            if (inMemoryAppenderImpl != null) {
                final Configuration config = ctx.getConfiguration();
                //
                if (name.equalsIgnoreCase(config.getLoggerConfig(name).getName())) {
                    config.removeLogger(name);
                }
                //
                AppenderRef ref = AppenderRef.createAppenderRef("InMemoryAppenderImplAppenderRef", level, null);
                AppenderRef[] refs = new AppenderRef[] {ref};
                LoggerConfig loggerConfig = LoggerConfig.createLogger(append, level, name, "true", refs, null, config, null);
                loggerConfig.addAppender(inMemoryAppenderImpl, null, null);
                config.addLogger(name, loggerConfig);
                ctx.updateLoggers();
            }
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    /**
     * Frees the given logger from the appender used to be displayed directly by this controller.
     *
     * @param name the name
     * @return the response entity
     */
    @RequestMapping(value = "free/{name}/", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET,
        headers = "Accept=application/json")
    @ResponseBody
    public ResponseEntity<LogResponse> free(@PathVariable("name")
    final String name) {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        synchronized (ctx) {
            final Configuration config = ctx.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(name);
            if (name.equalsIgnoreCase(loggerConfig.getName())) {
                config.removeLogger(name);
                LoggerConfig newloggerConfig = new LoggerConfig(name, loggerConfig.getLevel(), true);
                config.addLogger(name, newloggerConfig);
            }
            ctx.updateLoggers();
        }
        return new ResponseEntity<>(listLoggers(ctx), HttpStatus.OK);
    }

    @RequestMapping(value = "captured", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET, headers = "Accept=application/json")
    public void captured(HttpServletResponse response) {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        synchronized (ctx) {
            if (inMemoryAppenderImpl != null) {
                inMemoryAppenderImpl.getRegisteredEvents().stream().forEach(line -> {
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
