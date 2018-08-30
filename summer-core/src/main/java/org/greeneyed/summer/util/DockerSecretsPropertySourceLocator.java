package org.greeneyed.summer.util;

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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@ConditionalOnProperty(value = "summer.docker_secrets.enabled")
public class DockerSecretsPropertySourceLocator implements PropertySourceLocator {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
	public PropertySource<?> locate(Environment environment) {
		Map<String, Object> map = new HashMap<>();
		File secretsDirectory = new File("/run/secrets/");
		log.info("The secrets directory {} does{}exist", secretsDirectory.getAbsolutePath(),
				secretsDirectory.exists() ? " " : " not ");
		if (secretsDirectory.exists()) {
			for (File secret : secretsDirectory.listFiles()) {
				log.info("Found secret: {}", secret.getName());
				Properties secretProperties = new Properties();
				try (Reader reader = new FileReader(secret)) {
					secretProperties.load(reader);
					map.putAll(new HashMap(secretProperties));
				} catch (IOException e) {
					log.error("Error reading secrets file: {}", e.getMessage());
				}
			}
		}
		return new MapPropertySource("docker-secrets", map);
	}
}
