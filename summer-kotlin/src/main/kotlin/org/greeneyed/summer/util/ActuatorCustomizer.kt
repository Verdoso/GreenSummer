package org.greeneyed.summer.util

import java.util.LinkedHashSet

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.springframework.boot.actuate.endpoint.mvc.ActuatorMediaTypes
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

@Component
class ActuatorCustomizer : EndpointHandlerMappingCustomizer {

    internal class Fix : HandlerInterceptorAdapter() {
        @Throws(Exception::class)
        override fun preHandle(request: HttpServletRequest?, response: HttpServletResponse?, handler: Any?): Boolean {
            val attribute = request!!.getAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE)
            if (attribute is LinkedHashSet<*>) {
                @Suppress("UNCHECKED_CAST")
                val lhs = attribute as LinkedHashSet<MediaType>
                if (lhs.remove(ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON)) {
                    lhs.add(ActuatorMediaTypes.APPLICATION_ACTUATOR_V1_JSON)
                }
            }
            return true
        }
    }

    override fun customize(mapping: EndpointHandlerMapping) = mapping.setInterceptors(Fix())
}