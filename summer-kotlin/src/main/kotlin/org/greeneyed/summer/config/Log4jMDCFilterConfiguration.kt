package org.greeneyed.summer.config

import org.greeneyed.summer.filter.Log4jMDCFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Class Log4jMDCFilterConfiguration.
 */
@Configuration
open class Log4jMDCFilterConfiguration {
    /**
     * Servlet registration bean.

     * @return the filter registration bean
     */
    @Bean
    open fun servletRegistrationBean(): FilterRegistrationBean {
        val registrationBean = FilterRegistrationBean()
        val log4jMDCFilterFilter = Log4jMDCFilter()
        registrationBean.filter = log4jMDCFilterFilter
        registrationBean.order = 2
        return registrationBean
    }
}
