package org.greeneyed.summer.util

import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Collections

import org.springframework.cloud.bootstrap.config.PropertySourceLocator
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource

import mu.KotlinLogging

class SelfDiscoveryPropertySourceLocator : PropertySourceLocator {

    private val log = KotlinLogging.logger {}

    override fun locate(environment: Environment): PropertySource<*> {
        var result = MapPropertySource("SelfDiscoveredProperty", emptyMap<String, Any>())
        try {
            val localhostName = InetAddress.getLocalHost().canonicalHostName
            if (localhostName != null) {
                val hostname = localhostName.toLowerCase()
                log.info("Setting hostname to {}", hostname)
                result = MapPropertySource("SelfDiscoveredProperty",
                        Collections.singletonMap<String, Any>("spring.cloud.consul.discovery.hostname", hostname))
            }
        } catch (e: UnknownHostException) {
            log.error("Error obtaining localhost name", e)
        }

        return result
    }

}
