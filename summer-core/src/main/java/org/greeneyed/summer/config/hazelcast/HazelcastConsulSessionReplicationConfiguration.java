package org.greeneyed.summer.config.hazelcast;

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
import java.util.List;
import java.util.Properties;

import org.bitsofinfo.hazelcast.discovery.consul.BaseRegistrator;
import org.bitsofinfo.hazelcast.discovery.consul.ConsulDiscoveryConfiguration;
import org.bitsofinfo.hazelcast.discovery.consul.ConsulDiscoveryStrategyFactory;
import org.bitsofinfo.hazelcast.discovery.consul.HttpHealthCheckBuilder;
import org.bitsofinfo.hazelcast.discovery.consul.LocalDiscoveryNodeRegistrator;
import org.bitsofinfo.hazelcast.discovery.consul.TcpHealthCheckBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
import com.hazelcast.web.SessionListener;
import com.hazelcast.web.WebFilter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Data
@Slf4j
public class HazelcastConsulSessionReplicationConfiguration implements ApplicationContextAware {

    public static final String DEFAULT_SERVICE_PREFIX = "hz-";

    public static final String DEFAULT_DISCOVERY_DELAY = "3000";
    
    public static enum HEALTHCHECKTYPE {HTTP,TCP};
    
    public static final String DEFAULT_HEALTHCHECK_TYPE = "TCP";//HEALTHCHECK_TYPE.TCP.name();

    @Value("${spring.cloud.consul.host:localhost}")
    private String consulHost;

    @Value("${spring.cloud.consul.discovery.hostname:localhost}")
    private String discoveryHostName;

    @Value("${spring.cloud.consul.port:8500}")
    private String consulPort;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${summer.hazelcast.consul.configuration.delay:" + DEFAULT_DISCOVERY_DELAY + "}")
    private String configurationDelay;

    @Value("${summer.hazelcast.consul.service.prefix:" + DEFAULT_SERVICE_PREFIX + "}")
    private String servicePrefix;

    @Value("${summer.hazelcast.consul.service.tags:}")
    private String serviceTags;

    @Value("${summer.hazelcast.consul.healthcheck.type:" + DEFAULT_HEALTHCHECK_TYPE + "}")
    private HEALTHCHECKTYPE healthCheckType;
    
    @Value("${summer.hazelcast.consul.healthcheck.interval:10}")
    private int healthCheckInterval;
    
    @Value("${summer.hazelcast.consul.session_filter_priority:0}")
    private int sessionFilterPriority;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    /**
     * All {@link HazelcastConfigurer} Beans to further customize Hazelcast configuration. If
     * spring does not find any matching bean, then the List is {@code null}!.
     */
    @Autowired(required = false)
    private List<HazelcastConfigurer> hazelcastConfigurers;

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
        if(healthCheckType==HEALTHCHECKTYPE.HTTP)
        {
            config.setProperty("hazelcast.http.healthcheck.enabled", Boolean.TRUE.toString());            
        }
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

        switch(healthCheckType) {
            case HTTP:
                jsonRegistratorConfig.put(BaseRegistrator.CONFIG_PROP_HEALTH_CHECK_PROVIDER,HttpHealthCheckBuilder.class.getName());
                jsonRegistratorConfig.put(HttpHealthCheckBuilder.CONFIG_PROP_HEALTH_CHECK_HTTP, "http://#MYIP:#MYPORT/hazelcast/health");
                jsonRegistratorConfig.put(HttpHealthCheckBuilder.CONFIG_PROP_HEALTH_CHECK_HTTP_INTERVAL_SECONDS, healthCheckInterval);
                log.debug("Hazelcast HTTP health check set up (run every {} secs)", healthCheckInterval);
                break;
            case TCP:
                jsonRegistratorConfig.put(BaseRegistrator.CONFIG_PROP_HEALTH_CHECK_PROVIDER,TcpHealthCheckBuilder.class.getName());
                jsonRegistratorConfig.put(TcpHealthCheckBuilder.CONFIG_PROP_HEALTH_CHECK_TCP, "#MYIP:#MYPORT");
                jsonRegistratorConfig.put(TcpHealthCheckBuilder.CONFIG_PROP_HEALTH_CHECK_TCP_INTERVAL_SECONDS, healthCheckInterval);
                log.debug("Hazelcast TCP health check set up (run every {} secs)", healthCheckInterval);
                break;
            default:
                log.warn("What are you doing here, my oh my!");
                break;
        }
        

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
        
        
        // Apply custom configurations, if necessary
        if(hazelcastConfigurers!=null)
        {
            for(HazelcastConfigurer hazelcastConfigurer: hazelcastConfigurers)
            {
                log.debug("Applying HazelcastConfigurer {}", hazelcastConfigurer.getClass().getName());
                hazelcastConfigurer.configure(config);
            }
        }
        
        return config;
    }

    /**
     * Creates a Hazelcast web filter.
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
     * @return The web filter registration
     */    
    @Bean
    @ConditionalOnBean(name="hazlecastConsulConfig")
    @ConditionalOnProperty(name = "summer.hazelcast.consul.session_replication", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean webFilterRegistrationBean(HazelcastInstance hazelcastInstance) {
        final FilterRegistrationBean assertionTLFilter = new FilterRegistrationBean();
        Properties properties = new Properties();
        properties.put("instance-name", hazelcastInstance.getName());
        properties.put("sticky-session", Boolean.FALSE.toString());
        log.info("Session replication through Hazelcast registered!");
        assertionTLFilter.setFilter(new WebFilter(properties));
        assertionTLFilter.setOrder(sessionFilterPriority);
        return assertionTLFilter;
    }
    
    @Bean
    @ConditionalOnBean(name="hazlecastConsulConfig")
    @ConditionalOnProperty(name = "summer.hazelcast.consul.session_replication", havingValue = "true", matchIfMissing = true)
    public SessionListener hazelcastSessionListener() {
        return new SessionListener();
    }
}
