package org.greeneyed.summer.config

import mu.KotlinLogging
import org.greeneyed.summer.util.CustomXMLHttpMessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter

open class SummerWebConfig: WebMvcConfigurerAdapter() {

    private val log = KotlinLogging.logger {}

    @Value("\${summer.xml_http.poolsMaxPerKey:10}")
    private val poolsMaxPerKey: Int = 0

    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>?) {
        log.debug("Configuring XMLHttpMessageConverter with a pool of {} per key", poolsMaxPerKey)
        converters!!.add(CustomXMLHttpMessageConverter(poolsMaxPerKey))
        super.configureMessageConverters(converters)
    }

}