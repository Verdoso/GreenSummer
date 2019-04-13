package org.greeneyed.summer.config;

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


import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Class used to auto-configure Caffeine caches based on the name of the caches
 *
 */
@Configuration
@ConfigurationProperties(prefix = "summer.caching")
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "caffeine", matchIfMissing = true)
@Data
@Slf4j
public class CacheConfiguration {

    private Map<String, CacheSpec> specs;

    /**
     * Cache specification class that defines the parameters to see for the named cache
     *
     */
    @Data
    public static class CacheSpec {
        private Integer timeout;
        private Integer max = 200;
    }


    /**
     * The cache manager Bean, that defines caching parameters
     *
     * @param ticker The Ticker handle that Spring passes, unused so far
     * @return the cache manager
     */
    @Bean
    public CacheManager cacheManager(Ticker ticker) {
        SimpleCacheManager manager = new SimpleCacheManager();
        if (specs != null) {
            //@formatter:off
			List<CaffeineCache> caches =
					specs.entrySet().stream()
						.map(entry -> buildCache(entry.getKey(), entry.getValue(), ticker))
						.collect(Collectors.toList());
			//@formatter:on
            manager.setCaches(caches);
        }
        return manager;
    }

    private CaffeineCache buildCache(String name, CacheSpec cacheSpec, Ticker ticker) {
        log.info("Cache {} specified timeout of {} min, max of {}", name, cacheSpec.getTimeout(), cacheSpec.getMax());
        //@formatter:off
		final Caffeine<Object, Object> caffeineBuilder
				= Caffeine.newBuilder()
					.expireAfterWrite(cacheSpec.getTimeout(), TimeUnit.MINUTES)
					.maximumSize(cacheSpec.getMax())
					.ticker(ticker);
		//@formatter:on
        return new CaffeineCache(name, caffeineBuilder.build());
    }

    @Bean
    /**
     * The ticker bean, in case you have to do something time based in the cache manager
     *
     * @return the ticker bean
     */
    public Ticker ticker() {
        return Ticker.systemTicker();
    }
}
