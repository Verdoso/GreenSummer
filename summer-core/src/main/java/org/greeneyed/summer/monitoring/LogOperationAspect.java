package org.greeneyed.summer.monitoring;

/*-
 * #%L
 * GreenSummer
 * %%
 * Copyright (C) 2018 - 2020 GreenEyed (Daniel Lopez)
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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.security.Principal;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Supplier;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    //@formatter:off
    @Around("(" + "@annotation(org.springframework.web.bind.annotation.RequestMapping)"
            + "|| @annotation(org.springframework.web.bind.annotation.GetMapping)"
            + "|| @annotation(org.springframework.web.bind.annotation.PostMapping)"
            + "|| @annotation(org.springframework.web.bind.annotation.PutMapping)"
            + "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)"
            + "|| @annotation(org.springframework.web.bind.annotation.PatchMapping)"
            + "|| @annotation(org.greeneyed.summer.monitoring.LogOperation)"
            + ")" + "&& !@annotation(org.greeneyed.summer.monitoring.DontLogOperation)")
    //@formatter:on
    public Object logMethodCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getStaticPart().getSignature();
        Method method = methodSignature.getMethod();
        final String packageName = method.getDeclaringClass().getPackage().getName();
        Throwable errorProduced = null;
        Object result = null;
        if (packages == null || packages.stream().anyMatch(packageName::startsWith)) {
            Object[] args = joinPoint.getArgs();
            String userID = null;
            if (args.length > 0 && args[0] != null) {
                if (args[0] instanceof IdentifiedUser) {
                    userID = ((IdentifiedUser) args[0]).getName();
                } else if (args[0] instanceof Supplier) {
                    final Object suppliedValue = ((Supplier<?>) args[0]).get();
                    userID = suppliedValue != null ? suppliedValue.toString() : null;
                } else if (args[0] instanceof Principal) {
                    userID = ((Principal) args[0]).getName();
                }
            }
            try {
                if (userID != null) {
                    MDC.put(PRINCIPAL_LABEL, userID);
                }
                result = joinPoint.proceed();
            } catch (Throwable t) {
                errorProduced = t;
                throw t;
            } finally {
                if (logOperations) {
                    final String operationName = extractOperationName(method);
                    if (operationName != null) {
                        try {
                            MDC.put(OPERATION_LABEL, operationName);
                            StringBuilder theSB = extractArguments(method, args);
                            if (errorProduced != null) {
                                log.error("Performed: {}{}|{}", operationName, theSB.toString(), errorProduced.getClass().getSimpleName());
                            } else if (result != null && result instanceof ResponseEntity) {
                                log.info("Performed: {}{}|{}", operationName, theSB.toString(), ((ResponseEntity<?>) result).getStatusCodeValue());
                            } else {
                                log.info("Performed: {}{}", operationName, theSB.toString());
                            }
                        } finally {
                            MDC.remove(OPERATION_LABEL);
                        }
                    }
                }
                MDC.remove(PRINCIPAL_LABEL);
            }
        } else

        {
            result = joinPoint.proceed();
        }
        return result;
    }

    private StringBuilder extractArguments(Method method, Object[] args) {
        StringBuilder theSB = new StringBuilder();
        try {
            Parameter[] parameters = method.getParameters();
            boolean added = false;
            if (parameters != null && parameters.length > 0) {
                theSB.append(": ");
                for (int i = 0; i < parameters.length; i++) {
                    Parameter parameter = parameters[i];
                    if (parameter.getAnnotation(DontLog.class) == null && args[i] != null) {
                        final Class<?> parameterType = parameter.getType();
                        final boolean isLogable = Logable.class.isAssignableFrom(parameterType);
            //@formatter:off
                        if (
                            parameterType.isPrimitive()
                            || parameterType.isEnum()
                            || CharSequence.class.isAssignableFrom(parameterType)
                            || isLogable
                            ) {
                            addParameterName(theSB, added, parameter);
                            if(isLogable) {
                              theSB.append(((Logable)args[i]).formatted());
                            } else {
                              theSB.append(args[i].toString());
                            }
                            added = true;
                        } else if (
                                parameterType.isArray()
                                && (
                                    parameterType.getComponentType().isPrimitive()
                                    || parameterType.getComponentType().isEnum()
                                    || CharSequence.class.isAssignableFrom(parameterType.getComponentType())
                                )
                        ) {
                            addParameterName(theSB, added, parameter);
                            int lenght = Array.getLength(args[i]);
                            StringJoiner st = new StringJoiner(",");
                            for(int j = 0 ; j < lenght ; j++) {
                              st.add(Array.get(args[i], j).toString());
                            }
                            theSB.append(st.toString());
                            added = true;
                        }
                        //@formatter:on
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting arguments from method", e);
        }
        return theSB;
    }

    private void addParameterName(StringBuilder theSB, boolean added, Parameter parameter) {
        if (added) {
            theSB.append(";");
        }
        theSB.append(parameter.getName());
        theSB.append("=");
    }

    private String extractOperationName(Method method) {
        try {
            final String operationName;
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            PutMapping putMapping = method.getAnnotation(PutMapping.class);
            DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
            PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
            LogOperation logOperation = method.getAnnotation(LogOperation.class);
            RequestMapping classrequestMapping = method.getDeclaringClass().getAnnotation(RequestMapping.class);
            String basicName = null;
            if (logOperation != null && logOperation.value() != null && logOperation.value().trim().length() > 0) {
                operationName = logOperation.value();
            } else {
                if (requestMapping != null && requestMapping.value() != null && requestMapping.value().length > 0) {
                    basicName = requestMapping.value()[0];
                } else if (getMapping != null && getMapping.value() != null && getMapping.value().length > 0) {
                    basicName = getMapping.value()[0];
                } else if (postMapping != null && postMapping.value() != null && postMapping.value().length > 0) {
                    basicName = postMapping.value()[0];
                } else if (putMapping != null && putMapping.value() != null && putMapping.value().length > 0) {
                    basicName = putMapping.value()[0];
                } else if (deleteMapping != null && deleteMapping.value() != null && deleteMapping.value().length > 0) {
                    basicName = deleteMapping.value()[0];
                } else if (patchMapping != null && patchMapping.value() != null && patchMapping.value().length > 0) {
                    basicName = patchMapping.value()[0];
                }
                if (basicName != null) {
                    if (classrequestMapping != null && classrequestMapping.value() != null && classrequestMapping.value().length > 0) {
                        operationName = classrequestMapping.value()[0] + basicName;
                    } else {
                        operationName = basicName;
                    }
                } else {
                    operationName = null;
                }
            }
            return operationName;
        } catch (Exception e) {
            log.error("Error extracting arguments from method", e);
            return null;
        }
    }
}
