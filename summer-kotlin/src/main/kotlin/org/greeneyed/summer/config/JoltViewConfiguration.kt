package org.greeneyed.summer.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.greeneyed.summer.util.SummerJoltView
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.ModelAndView


@Configuration
@ConfigurationProperties(prefix = "summer.jolt")
class JoltViewConfiguration {

    companion object {
        const val DEFAULT_SPEC_PREFIX = "/json-spec/"
        const val JSON_SOURCE_TAG = "jsonSource"
        const val REFRESH_SPEC_FLAG = "refreshSpec"
        const val SHOW_JSON_SOURCE_FLAG = "showJsonSource"
        const val DEFAULT_SPEC_SUFFIX = ".json"
    }

    val devMode = false
    val specPrefix = DEFAULT_SPEC_PREFIX
    val specSuffix = DEFAULT_SPEC_SUFFIX
}