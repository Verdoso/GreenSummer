package org.greeneyed.summer.config

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.util.StringUtils
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import java.util.*

@Configuration
open class SummerGlobalConfiguration {

    companion object {
        const val CLASSPATH_PREFIX = "classpath:"
    }

    private val log = KotlinLogging.logger {}

    @Value("\${labels.fileName:labels}")
    var fileName: String? = null

    @Value("\${labels.languageParameter:language}")
    var languageParameter: String? = null

    @Value("\${labels.defaultLocale:}")
    var defaultLocale: String? = null

    @Value("\${labels.reload:false}")
    var reloadLabels: Boolean = false

    @Bean
    open fun localeResolver(): SessionLocaleResolver {
        val localeResolver = SessionLocaleResolver()
        if (!StringUtils.isEmpty(defaultLocale)) {
            localeResolver.setDefaultLocale(Locale(defaultLocale))
            log.info("Labels default language set to {} ", defaultLocale);
        }
        return localeResolver
    }

    @Bean
    open fun localeChangeInterceptor(): LocaleChangeInterceptor {
        val localeChangeInterceptor = LocaleChangeInterceptor()
        localeChangeInterceptor.setParamName(languageParameter)
        return localeChangeInterceptor;
    }

    @Bean
    open fun messageSource(): MessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setBasename(CLASSPATH_PREFIX + fileName);
        // if true, the key of the message will be displayed if the key is not
        // found, instead of throwing a NoSuchMessageException
        messageSource.setUseCodeAsDefaultMessage(false);
        messageSource.setDefaultEncoding("UTF-8");
        // // # -1 : never reload, 0 always reload
        log.info("Labels {} be reloaded", if (reloadLabels) "will" else "won't");
        messageSource.setCacheSeconds(if (reloadLabels) 0 else -1);
        return messageSource;
    }
}