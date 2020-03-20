package org.greeneyed.summer.controller;

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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpoint.PropertySourceDescriptor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Requires management.endpoints.web.exposure.include to ,at least, have configInspector and configprops 
 *
 */
@Component
@Endpoint(id = "configInspector")
@Slf4j
public class ConfigInspectorEndpoint {

    private static final String ENCRYPTED_TOKEN = "*******************";
    private static List<String> EXCLUDED_MAPS = Arrays.asList("systemProperties", "systemEnvironment", "server.ports", "servletContextInitParams");
    private static List<String> ENCRIPTED_MAPS = Arrays.asList("decrypted");
    private static List<String> ENCRIPTED_KEYS = Arrays.asList("password", "secret", "key");

    @Autowired
    private EnvironmentEndpoint envEndpoint;

    @ReadOperation(produces = MediaType.TEXT_PLAIN_VALUE)
    public String propertySources() throws IOException {
        try (StringWriter theSW = new StringWriter(); BufferedWriter theBW = new BufferedWriter(theSW)) {
            theBW.write("# Excluded sources from display: \n#  ");
            theBW.write(EXCLUDED_MAPS.stream().collect(Collectors.joining("\n#  ")));
            theBW.newLine();
            theBW.newLine();
            theBW.write("# Sources included, in order of preference ");
            theBW.newLine();
            envEndpoint.environment(null).getPropertySources().forEach(propertySourceDescriptor -> {
                try {
                    if (!EXCLUDED_MAPS.contains(propertySourceDescriptor.getName())) {
                        theBW.write(propertySourceDescriptor.getName());
                        theBW.newLine();
                    }
                } catch (IOException e) {
                    log.error("Error printing map name, doh!", e);
                }
            });
            theBW.flush();
            return theSW.toString();
        }
    }
   
    @ReadOperation(produces = MediaType.TEXT_PLAIN_VALUE)
    public String properties(@Selector String format) throws IOException {
        if("yaml".equalsIgnoreCase(format)) {
            return returnPropertiesAsString(false);
        } else {            
            return returnPropertiesAsString(true);
        }
        
    }

    private String returnPropertiesAsString(boolean asProperties) throws IOException {
        Map<String, List<String[]>> finalValues = obtainFinalValuesAndOrigin();

        try (StringWriter theSW = new StringWriter(); BufferedWriter theBW = new BufferedWriter(theSW)) {
            finalValues.forEach((origin, values) -> {
                try {
                    final boolean encrypted = ENCRIPTED_MAPS.contains(origin);
                    theBW.write("# Derived from: ");
                    theBW.write(origin);
                    theBW.newLine();
                    if (asProperties) {
                        printPropertyValues(theBW, values, encrypted);
                    } else {
                        printYamlValues(theBW, values, encrypted);
                    }
                    theBW.newLine();
                } catch (IOException e) {
                    log.error("Error printing values", e);
                }
            });
            theBW.flush();
            return theSW.toString();
        }
    }


    private Map<String, List<String[]>> obtainFinalValuesAndOrigin() {
        List<PropertySourceDescriptor> propertyMap = envEndpoint.environment(null).getPropertySources();
        Set<String> keys = new TreeSet<>();
        List<PropertySourceDescriptor> propertyMaps = new ArrayList<>();
        Map<String, List<String[]>> finalValues = new HashMap<>();
        findPropertyKeysFromMap(propertyMap, keys, propertyMaps);
        keys.stream().forEach(key -> {
            String finalValue = "Final value";
            String origin = "unknown";
            for (PropertySourceDescriptor map : propertyMaps) {
                log.debug("{}, checking inside: {}", key, map.getName());
                if (map.getProperties().containsKey(key)) {
                    origin = map.getName();
                    finalValue = map.getProperties().get(key).getValue().toString();
                    break;
                }
            }
            finalValues.putIfAbsent(origin, new ArrayList<>());
            finalValues.get(origin).add(new String[] {key, finalValue});
        });
        return finalValues;
    }

    private void printPropertyValues(BufferedWriter theBW, List<String[]> values, boolean encrypted) throws IOException {
        for (String[] pair : values) {
            theBW.write(pair[0]);
            theBW.write("=");
            writeValue(theBW, encrypted, pair);
            theBW.newLine();
        }
    }

    private void printYamlValues(BufferedWriter theBW, List<String[]> values, boolean encrypted) throws IOException {
        String previousKey = "";
        for (String[] pair : values) {
            printYamlKey(pair[0], previousKey, theBW);
            writeValue(theBW, encrypted, pair);
            theBW.newLine();
            previousKey = pair[0];
        }
    }

    private void writeValue(BufferedWriter theBW, boolean encrypted, String[] pair) throws IOException {
        if (encrypted || ENCRIPTED_KEYS.contains(pair[0])) {
            theBW.write(ENCRYPTED_TOKEN);
        } else {
            theBW.write(pair[1]);
        }
    }

    private void printYamlKey(String key, String previousKey, BufferedWriter theBW) throws IOException {
        int lastDot = key.lastIndexOf(".");
        if (lastDot > -1) {
            String prefix = key.substring(0, lastDot);
            // If the prefix of the previous key was different up to this point, print it,
            // else ignore it
            if (previousKey.length() <= lastDot || !previousKey.substring(0, lastDot).equals(prefix)) {
                printYamlKey(prefix, previousKey, theBW);
                theBW.newLine();
            }
            for (int i = 0; i < StringUtils.countOccurrencesOf(prefix, ".") + 1; i++) {
                theBW.write("  ");
            }
            theBW.write(key.substring(lastDot + 1));
        } else {
            theBW.write(key);
        }
        theBW.write(": ");
    }

    private void findPropertyKeysFromMap(List<PropertySourceDescriptor> propertyMap, Set<String> keys, List<PropertySourceDescriptor> propertyMaps) {
        for (PropertySourceDescriptor propertySourceDescriptor : propertyMap) {
            if (!EXCLUDED_MAPS.contains(propertySourceDescriptor.getName())) {
                propertyMaps.add(propertySourceDescriptor);
                keys.addAll(propertySourceDescriptor.getProperties().keySet());
            }
        }
    }
}
