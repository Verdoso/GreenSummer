package org.greeneyed.summer.filter

import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Component(value = "Log4UUIDFilter")
public class Log4jMDCFilter : OncePerRequestFilter() {

    companion object {
        const val RESPONSE_TOKEN_HEADER = "Response_Token"
        const val MDC_UUID_TOKEN_KEY = "Log4UUIDFilter.UUID"
    }

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse,
                                  chain: FilterChain) {
        try {
            val token = UUID.randomUUID().toString().toUpperCase().replace("-", "")
            MDC.put(MDC_UUID_TOKEN_KEY, token)
            response.addHeader(RESPONSE_TOKEN_HEADER, token)
            chain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_UUID_TOKEN_KEY)
        }
    }

    override fun isAsyncDispatch(request: HttpServletRequest) = false
    override fun shouldNotFilterErrorDispatch() = false

}
