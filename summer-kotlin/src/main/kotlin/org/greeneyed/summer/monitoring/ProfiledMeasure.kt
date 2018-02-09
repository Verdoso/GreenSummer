package org.greeneyed.summer.monitoring

import org.slf4j.MDC
data class ProfiledMeasure (val name: String? = null
                       , val value: Long = 0
                       , val isShowValue: Boolean = false
                       , val tags: Array<String>? = null
                       , val token: String? = MDC.get(ProfiledMeasure.MDC_UUID_TOKEN_KEY)) {
    companion object {
        val MDC_UUID_TOKEN_KEY = "Log4UUIDFilter.UUID"
    }
}
