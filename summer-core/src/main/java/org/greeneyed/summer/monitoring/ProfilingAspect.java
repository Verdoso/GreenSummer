package org.greeneyed.summer.monitoring;

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


import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.greeneyed.summer.config.Slf4jMDCFilterConfiguration;
import org.greeneyed.summer.monitoring.ProfiledMeasure.ProfiledMeasureBuilder;
import org.springframework.beans.factory.annotation.Value;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
public abstract class ProfilingAspect {
    private long threshold = 10;
    private String[] configuredTags = new String[0];
    private Thread loggingWorkerThread = null;
    private LoggingWorker loggingWorker = null;
    private Predicate<HttpServletRequest> requestFilter;

    @Value("${spring.application.name}")
    private String serviceName;

    public void configure(long threshold, String... tags) {
        this.threshold = threshold;
        configuredTags = tags;
        loggingWorker = new LoggingWorker(log);
        loggingWorkerThread = new Thread(loggingWorker);
        loggingWorkerThread.setDaemon(true);
        loggingWorkerThread.start();
        log.info("Profiling aspect started with tags {} and threshold {}", configuredTags, threshold);
        requestFilter = getRequestFilter();
    }

    protected Predicate<HttpServletRequest> getRequestFilter() {
        return r -> r.getRequestURI() != null;
    }

    public void stopProfiling() {
        if (loggingWorkerThread != null) {
            loggingWorker.signalToStop();
            loggingWorkerThread.interrupt();
        }
    }

    @Pointcut("@annotation(org.greeneyed.summer.monitoring.Measured)")
    public void measuredMethods() {
        // Pointcut methods need no body
    }

    @Pointcut("@annotation(org.greeneyed.summer.monitoring.Counted)")
    public void countedMethods() {
        // Pointcut methods need no body
    }

    @Pointcut("execution(@org.springframework.web.bind.annotation.RequestMapping * *(..))")
    public void requestMappings() {
        // Pointcut methods need no body
    }

    @Pointcut("execution(* org.eclipse.jetty.servlet.ServletHandler.doHandle(..))")
    public void servletHandlerHandle() {
        // Pointcut methods need no body
    }

    @Around("servletHandlerHandle()")
    public Object profileJettyContainer(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        HttpServletRequest request = (HttpServletRequest) proceedingJoinPoint.getArgs()[2];
        final HttpServletResponse response = (HttpServletResponse) proceedingJoinPoint.getArgs()[3];
        if (requestFilter.test(request)) {
            return measure(proceedingJoinPoint, buildNameForRequest(request),
                //TODO: Careful, if the name of the header is changed through properties, this won't get it!
                () -> response.getHeader(Slf4jMDCFilterConfiguration.DEFAULT_RESPONSE_TOKEN_HEADER));
        } else {
            return proceedingJoinPoint.proceed();
        }
    }

    protected String buildNameForRequest(HttpServletRequest request) {
        String name = request.getRequestURI().substring(1);
        int nextSlash = name.indexOf('/', 1);
        if (nextSlash > -1) {
            name = name.substring(0, nextSlash);
        }
        return "Container.service." + name + "_" + request.getMethod();
    }

    public Object measure(ProceedingJoinPoint proceedingJoinPoint, String name, Supplier<String> tokenExtracter) throws Throwable {
        final long startTime = System.nanoTime();
        try {
            return proceedingJoinPoint.proceed();
        } finally {
            final long timeSpent = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            if (timeSpent >= threshold) {
                time(name, tokenExtracter != null ? tokenExtracter.get() : null, timeSpent);
            }
        }
    }

    public Object measure(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return measure(proceedingJoinPoint, getMessageString(proceedingJoinPoint), null);
    }

    public Object count(ProceedingJoinPoint proceedingJoinPoint, Supplier<String> tokenExtracter) throws Throwable {
        try {
            return proceedingJoinPoint.proceed();
        } finally {
            String name = getMessage(proceedingJoinPoint).toString();
            count(name, tokenExtracter != null ? tokenExtracter.get() : null);
        }
    }

    public Object count(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        return count(proceedingJoinPoint, null);
    }

    protected void processMeasure(ProfiledMeasure measure) {
        if (log.isTraceEnabled()) {
            loggingWorker.enqueue(measure);
        }
    }

    private String[] joinTags(String[] tags, String[] extraTags) {
        final String[] result;
        if (tags == null) {
            result = extraTags;
        } else if (extraTags == null) {
            result = tags;
        } else {
            result = new String[tags.length + extraTags.length];
            System.arraycopy(tags, 0, result, 0, tags.length);
            System.arraycopy(extraTags, 0, result, tags.length, extraTags.length);
        }
        return result;
    }

    protected void count(final String name, final String token, final String... tags) {
        ProfiledMeasureBuilder measure = ProfiledMeasure.builder().name(name).value(1L).tags(joinTags(configuredTags, tags));
        if (token != null) {
            measure.token(token);
        }
        processMeasure(measure.build());
    }

    protected void time(final String name, final String token, final long value, final String... tags) {
        ProfiledMeasureBuilder measure = ProfiledMeasure.builder().name(name).value(value).tags(joinTags(configuredTags, tags)).showValue(true);
        if (token != null) {
            measure.token(token);
        }
        processMeasure(measure.build());
    }

    protected static String getMessageString(JoinPoint joinPoint) {
        return getMessage(joinPoint).toString();
    }

    protected static StringBuilder getMessage(JoinPoint joinPoint) {
        final StringBuilder messageSB = new StringBuilder();
        if (joinPoint.getTarget() != null) {
            messageSB.append(joinPoint.getTarget().getClass().getSimpleName());
        } else {
            messageSB.append(joinPoint.getSignature().getDeclaringType().getSimpleName());
        }
        if (joinPoint.getSignature() != null) {
            messageSB.append('.');
            String name = joinPoint.getSignature().getName();
            if (joinPoint.getSignature() instanceof MethodSignature) {
                MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                Method targetMethod = methodSignature.getMethod();
                Measured measured = targetMethod.getAnnotation(Measured.class);
                if (measured != null && measured.value() != null && measured.value().trim().length() > 0) {
                    name = measured.value();
                } else {
                    Counted counted = targetMethod.getAnnotation(Counted.class);
                    if (counted != null && counted.value() != null && counted.value().trim().length() > 0) {
                        name = counted.value();
                    }
                }
            }
            messageSB.append(name);
        }
        return messageSB;
    }
}
