package org.greeneyed.summer.config;

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

import java.net.InetAddress;
import java.util.Properties;

import org.bitsofinfo.hazelcast.discovery.consul.BaseRegistrator;
import org.bitsofinfo.hazelcast.discovery.consul.ConsulDiscoveryConfiguration;
import org.bitsofinfo.hazelcast.discovery.consul.ConsulDiscoveryStrategyFactory;
import org.bitsofinfo.hazelcast.discovery.consul.LocalDiscoveryNodeRegistrator;
import org.bitsofinfo.hazelcast.discovery.consul.TcpHealthCheckBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hazelcast.config.Config;
import com.hazelcast.config.DiscoveryStrategyConfig;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.context.SpringManagedContext;
import com.hazelcast.web.WebFilter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Data
@Slf4j
public class HazelcastConsulSessionReplicationConfiguration implements ApplicationContextAware {

    public static final String DEFAULT_SERVICE_PREFIX = "hz-";

    public static final String DEFAULT_DISCOVERY_DELAY = "3000";

    @Value("${spring.cloud.consul.host:localhost}")
    String consulHost;

    @Value("${spring.cloud.consul.discovery.hostname:localhost}")
    String discoveryHostName;

    @Value("${spring.cloud.consul.port:8500}")
    String consulPort;

    @Value("${spring.application.name}")
    String appName;

    @Value("${summer.hazelcast.consul.configuration.delay:" + DEFAULT_DISCOVERY_DELAY + "}")
    String configurationDelay;

    @Value("${summer.hazelcast.consul.service.prefix:" + DEFAULT_SERVICE_PREFIX + "}")
    String servicePrefix;

    @Value("${summer.hazelcast.consul.service.tags:}")
    String serviceTags;

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @ConditionalOnProperty(name = "summer.hazelcast.consul.enabled", havingValue = "true", matchIfMissing = false)
    public Config hazlecastConsulConfig() throws Exception {
        Config config = new XmlConfigBuilder().build();
        //
        final SpringManagedContext springManagedContext = new SpringManagedContext();
        springManagedContext.setApplicationContext(applicationContext);
        config.setManagedContext(springManagedContext);
        //
        // Use Consul for discovery instead of multicast with the help of this:
        // https://github.com/bitsofinfo/hazelcast-consul-discovery-spi
        //
        config.setProperty("hazelcast.discovery.enabled", Boolean.TRUE.toString());
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().setPublicAddress(InetAddress.getByName(discoveryHostName).getHostAddress());

        DiscoveryStrategyConfig discoveryStrategyConfig = new DiscoveryStrategyConfig(
                new ConsulDiscoveryStrategyFactory());
        discoveryStrategyConfig.addProperty(ConsulDiscoveryConfiguration.CONSUL_HOST.key(), this.consulHost);
        discoveryStrategyConfig.addProperty(ConsulDiscoveryConfiguration.CONSUL_PORT.key(), this.consulPort);
        discoveryStrategyConfig.addProperty(ConsulDiscoveryConfiguration.CONSUL_SERVICE_TAGS.key(), this.serviceTags);
        discoveryStrategyConfig.addProperty(ConsulDiscoveryConfiguration.CONSUL_SERVICE_NAME.key(),
                this.servicePrefix + this.appName);
        discoveryStrategyConfig.addProperty(ConsulDiscoveryConfiguration.CONSUL_HEALTHY_ONLY.key(), true);
        discoveryStrategyConfig.addProperty(ConsulDiscoveryConfiguration.CONSUL_DISCOVERY_DELAY_MS.key(),
                this.configurationDelay);

        discoveryStrategyConfig.addProperty(ConsulDiscoveryConfiguration.CONSUL_REGISTRATOR.key(),
                LocalDiscoveryNodeRegistrator.class.getName());
        ObjectNode jsonRegistratorConfig = JsonNodeFactory.instance.objectNode();
        jsonRegistratorConfig.put(LocalDiscoveryNodeRegistrator.CONFIG_PROP_PREFER_PUBLIC_ADDRESS, true);

        jsonRegistratorConfig.put(BaseRegistrator.CONFIG_PROP_HEALTH_CHECK_PROVIDER,
                TcpHealthCheckBuilder.class.getName());
        jsonRegistratorConfig.put(TcpHealthCheckBuilder.CONFIG_PROP_HEALTH_CHECK_TCP, "#MYIP:#MYPORT");
        jsonRegistratorConfig.put(TcpHealthCheckBuilder.CONFIG_PROP_HEALTH_CHECK_TCP_INTERVAL_SECONDS, 10);

        // Scripts are executed on the consul server, so they are consul-host
        // dependent, meh
        // jsonRegistratorConfig.put(BaseRegistrator.CONFIG_PROP_HEALTH_CHECK_PROVIDER,
        // ScriptHealthCheckBuilder.class.getName());
        // jsonRegistratorConfig.put(ScriptHealthCheckBuilder.CONFIG_PROP_HEALTH_CHECK_SCRIPT,
        // "nc -z #MYIP #MYPORT");
        // jsonRegistratorConfig.put(ScriptHealthCheckBuilder.CONFIG_PROP_HEALTH_CHECK_SCRIPT_INTERVAL_SECONDS,
        // 10);

        discoveryStrategyConfig.addProperty(ConsulDiscoveryConfiguration.CONSUL_REGISTRATOR_CONFIG.key(),
                jsonRegistratorConfig.toString());

        config.getNetworkConfig().getJoin().getDiscoveryConfig().getDiscoveryStrategyConfigs()
                .add(discoveryStrategyConfig);
        log.info("Hazelcast configured to use Consul for discovery");
        return config;
    }

    /**
     * Create a web filter. Parameterize this with two properties,
     *
     * <ol>
     * <li><i>instance-name</i> Direct the web filter to use the existing
     * Hazelcast instance rather than to create a new one.</li>
     * <li><i>sticky-session</i> As the HTTP session will be accessed from
     * multiple processes, deactivate the optimization that assumes each user's
     * traffic is routed to the same process for that user.</li>
     * </ol>
     *
     * Spring will assume dispatcher types of {@code FORWARD}, {@code INCLUDE}
     * and {@code REQUEST}, and a context pattern of "{@code /*}".
     *
     * @param hazelcastInstance
     *            Created by Spring
     * @return The web filter for Tomcat
     */
    @Bean
    @ConditionalOnBean(name="hazlecastConsulConfig")
    @ConditionalOnProperty(name = "summer.hazelcast.consul.session_replication", havingValue = "true", matchIfMissing = true)
    public WebFilter webFilter(HazelcastInstance hazelcastInstance) {
        Properties properties = new Properties();
        properties.put("instance-name", hazelcastInstance.getName());
        properties.put("sticky-session", Boolean.FALSE.toString());
        log.info("Session replication through Hazelcast set!");
        return new WebFilter(properties);
    }
}