package org.greeneyed.summer.controller;

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
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping(value = "/manage/config_inspector")
@Slf4j
public class ConfigInspectorController {

    private static final String ENCRYPTED_TOKEN = "*******************";
    private static List<String> EXCLUDED_MAPS = Arrays.asList("systemProperties", "systemEnvironment", "server.ports", "servletContextInitParams");
    private static List<String> ENCRIPTED_MAPS = Arrays.asList("decrypted");
    private static List<String> ENCRIPTED_KEYS = Arrays.asList("password", "secret", "key");

    @Autowired
    private EnvironmentEndpoint envEndpoint;

    @Data
    @AllArgsConstructor
    private static class MapSpec {
        private String name;
        private Map<String, Object> map;
    }

    @RequestMapping(value = "/property_sources", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String propertySources() throws IOException {
        try (StringWriter theSW = new StringWriter(); BufferedWriter theBW = new BufferedWriter(theSW)) {
            theBW.write("# Excluded sources from display: \n#  ");
            theBW.write(EXCLUDED_MAPS.stream().collect(Collectors.joining("\n#  ")));
            theBW.newLine();
            theBW.newLine();
            theBW.write("# Sources included, in order of preference ");
            theBW.newLine();
            Map<String, Object> propertyMap = envEndpoint.invoke();
            propertyMap.forEach((key, value) -> {
                try {
                    if (value instanceof Map && !EXCLUDED_MAPS.contains(key)) {
                        theBW.write(key);
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

    @RequestMapping(value = "/pretty_properties", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String prettyProperties() throws IOException {
        return returnPropertiesAsHtml(true);
    }

    @RequestMapping(value = "/pretty_yaml", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String prettyYaml() throws IOException {
        return returnPropertiesAsHtml(false);
    }

    private String returnPropertiesAsHtml(boolean asProperties) throws IOException {
        Map<String, List<String[]>> finalValues = obtainFinalValuesAndOrigin();

        try (StringWriter theSW = new StringWriter(); BufferedWriter theBW = new BufferedWriter(theSW)) {
            theBW.write("<html>");
            theBW.write("<head>");
            theBW.write("<title>");
            theBW.write("Derived properties");
            theBW.write("</title>");
            theBW.write("</head>");
            theBW.write("<body>");
            finalValues.forEach((origin, values) -> {
                try {
                    final boolean encrypted = ENCRIPTED_MAPS.contains(origin);
                    theBW.write("<table style='min-width: 80%; margin-bottom: 2em;'>");
                    theBW.write("<caption style='font-size: 1.5em; text-align: left;'>");
                    theBW.write("Derived from: ");
                    theBW.write(origin);
                    theBW.write("</caption>");
                    if (asProperties) {
                        printPropertyHtmlValues(theBW, values, encrypted);
                    } else {
                        printYamlHtmlValues(theBW, values, encrypted);
                    }
                    theBW.write("</table>");
                } catch (IOException e) {
                    log.error("Error printing values", e);
                }
            });
            theBW.write("</body>");
            theBW.write("</html>");
            theBW.flush();
            return theSW.toString();
        }
    }

    @RequestMapping(value = "/properties", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String properties() throws IOException {
        return returnPropertiesAsString(true);
    }

    @RequestMapping(value = "/yaml", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public String yaml() throws IOException {
        return returnPropertiesAsString(false);
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
        Map<String, Object> propertyMap = envEndpoint.invoke();
        PropertyResolver propertyResolver = envEndpoint.getResolver();
        Set<String> keys = new TreeSet<>();
        List<MapSpec> propertyMaps = new ArrayList<>();
        Map<String, List<String[]>> finalValues = new HashMap<>();
        findPropertyKeysFromMap(propertyMap, keys, propertyMaps);
        keys.stream().filter(propertyResolver::containsProperty).forEach(key -> {
            String finalValue = propertyResolver.getProperty(key);
            String origin = "unknown";
            for (MapSpec map : propertyMaps) {
                log.debug("{}, checking inside: {}", key, map.getName());
                if (map.getMap().containsKey(key)) {
                    origin = map.getName();
                    break;
                }
            }
            finalValues.putIfAbsent(origin, new ArrayList<>());
            finalValues.get(origin).add(new String[] {
                key, finalValue});
        });
        return finalValues;
    }

    private void printPropertyHtmlValues(BufferedWriter theBW, List<String[]> values, boolean encrypted) throws IOException {
        for (String[] pair : values) {
            theBW.write("<tr>");
            theBW.write("<th style='width: 50%; text-align: left;'>");
            theBW.write(pair[0]);
            theBW.write("</th>");
            theBW.write("<td>");
            writeValue(theBW, encrypted, pair);
            theBW.write("</td>");
            theBW.write("</tr>");
        }
    }

    private void printPropertyValues(BufferedWriter theBW, List<String[]> values, boolean encrypted) throws IOException {
        for (String[] pair : values) {
            theBW.write(pair[0]);
            theBW.write("=");
            writeValue(theBW, encrypted, pair);
            theBW.newLine();
        }
    }

    private void printYamlHtmlValues(BufferedWriter theBW, List<String[]> values, boolean encrypted) throws IOException {
        String previousKey = "";
        for (String[] pair : values) {
            theBW.write("<tr>");
            theBW.write("<th style='margin: 0px; padding: 0px; width: 50%; text-align: left;'>");
            theBW.write("<pre  style='margin: 0'>");
            printYamlHtmlKey(pair[0], previousKey, theBW);
            theBW.write("</pre>");
            theBW.write("</th>");
            theBW.write("<td style='margin: 0px; padding: 0px;'>");
            writeValue(theBW, encrypted, pair);
            theBW.write("</td>");
            theBW.write("</tr>");
            previousKey = pair[0];
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
        if(encrypted || ENCRIPTED_KEYS.contains(pair[0])) {
            theBW.write(ENCRYPTED_TOKEN);
        } else {
            theBW.write(pair[1]);
        }
    }

    private void printYamlHtmlKey(String key, String previousKey, BufferedWriter theBW) throws IOException {
        int lastDot = key.lastIndexOf(".");
        if (lastDot > -1) {
            String prefix = key.substring(0, lastDot);
            key = key.substring(lastDot + 1);
            // If the prefix of the previous key was different up to this point, print it,
            // else ignore it
            if (previousKey.length() <= lastDot || !previousKey.substring(0, lastDot).equals(prefix)) {
                printYamlHtmlKey(prefix, previousKey, theBW);
                theBW.write("<br/>");
            }
            for (int i = 0; i < StringUtils.countOccurrencesOf(prefix, ".") + 1; i++) {
                theBW.write("  ");
            }
        }
        theBW.write(key);
        theBW.write(": ");
    }

    private void printYamlKey(String key, String previousKey, BufferedWriter theBW) throws IOException {
        int lastDot = key.lastIndexOf(".");
        if (lastDot > -1) {
            String prefix = key.substring(0, lastDot);
            key = key.substring(lastDot + 1);
            // If the prefix of the previous key was different up to this point, print it,
            // else ignore it
            if (previousKey.length() <= lastDot || !previousKey.substring(0, lastDot).equals(prefix)) {
                printYamlKey(prefix, previousKey, theBW);
                theBW.newLine();
            }
            for (int i = 0; i < StringUtils.countOccurrencesOf(prefix, ".") + 1; i++) {
                theBW.write("  ");
            }
        }
        theBW.write(key);
        theBW.write(": ");
    }

    @SuppressWarnings("unchecked")
    private void findPropertyKeysFromMap(Map<String, Object> propertyMap, Set<String> keys, List<MapSpec> propertyMaps) {
        propertyMap.forEach((key, value) -> {
            if (value instanceof Map) {
                if (!EXCLUDED_MAPS.contains(key)) {
                    propertyMaps.add(new MapSpec(key, (Map<String, Object>) value));
                    findPropertyKeysFromMap((Map<String, Object>) value, keys, propertyMaps);
                }
            } else {
                keys.add(key);
            }
        });
    }

}
