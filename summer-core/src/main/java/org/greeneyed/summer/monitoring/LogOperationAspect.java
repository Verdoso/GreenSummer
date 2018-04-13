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
import java.security.Principal;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
@Data
public class LogOperationAspect {

    private static final String OPERATION_LABEL = "Operation";
    private static final String PRINCIPAL_LABEL = "Principal";

    @Value("${summer.operation_logging.log_requests:false}")
    private boolean logOperations;
    @Value("#{'${summer.operation_logging.included_packages}'.split(',')}")
    private List<String> packages;

    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getStaticPart().getSignature();
        Method method = methodSignature.getMethod();
        final String packageName = method.getDeclaringClass().getPackage().getName();
        final Object result;
        if (packages == null || packages.stream().anyMatch(packageName::startsWith)) {
            Object[] args = joinPoint.getArgs();
            String userID = null;
            if (args.length > 0 && args[0] != null) {
                if (args[0] instanceof IdentifiedUser) {
                    userID = ((IdentifiedUser) args[0]).getName();
                } else if (args[0] instanceof Principal) {
                    userID = ((Principal) args[0]).getName();
                }
            }
            try {
                if (userID != null) {
                    MDC.put(PRINCIPAL_LABEL, userID);
                }
                if (logOperations) {
                    final String operationName = extractOperationName(method);
                    if (operationName != null) {
                        try {
                            MDC.put(OPERATION_LABEL, operationName);
                            log.info("Requested: {}", operationName);
                        } finally {
                            MDC.remove(OPERATION_LABEL);
                        }
                    }
                }
                result = joinPoint.proceed();
            } finally {
                MDC.remove(PRINCIPAL_LABEL);
            }
        } else {
            result = joinPoint.proceed();
        }
        return result;
    }

    private String extractOperationName(Method method) {
        RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
        RequestMapping classrequestMapping = method.getDeclaringClass().getAnnotation(RequestMapping.class);
        final String operationName;
        if (requestMapping != null && requestMapping.value() != null && requestMapping.value().length > 0) {
            if (classrequestMapping != null && classrequestMapping.value() != null && classrequestMapping.value().length > 0) {
                operationName = classrequestMapping.value()[0] + requestMapping.value()[0];
            } else {
                operationName = requestMapping.value()[0];
            }
        } else if (classrequestMapping != null && classrequestMapping.value() != null && classrequestMapping.value().length > 0) {
            operationName = classrequestMapping.value()[0];
        } else {
            // operationName = joinPoint.getSignature().getName();
            operationName = null;
        }
        return operationName;
    }
}
