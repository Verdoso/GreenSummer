package org.greeneyed.summer.config

import mu.KotlinLogging
import org.greeneyed.summer.util.SummerXSLTView
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.MessageSourceAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.View
import org.springframework.web.servlet.ViewResolver
import org.springframework.web.servlet.view.xslt.XsltViewResolver
import java.util.*

@Configuration
@ConfigurationProperties(prefix = "summer.xslt")
open class XsltConfiguration {

    open class XsltModelAndView: ModelAndView {
        constructor(viewName: String, modelObject: Any)
                : super(viewName + XsltConfiguration.XSLT_SUFFIX, XsltConfiguration.XML_SOURCE_TAG, modelObject)

        constructor(viewName: String, modelObject: Any, status: HttpStatus)
                : super(viewName + XsltConfiguration.XSLT_SUFFIX, status) {
            getModelMap().put(XsltConfiguration.XML_SOURCE_TAG, modelObject)
        }
    }

    open class CustomXsltViewResolver: XsltViewResolver() {
        override open fun loadView(viewName: String, locale: Locale): View {
            val view = super.loadView(viewName, locale)
            if (view is MessageSourceAware) {
                view.setMessageSource(applicationContext)
            }
            return view;
        }
    }

    companion object {
        const val XSLT_SUFFIX = ".xslt"
        const val XML_SOURCE_TAG = "xmlSource"
        const val REFRESH_XSLT_FLAG = "refreshXSLT"
        const val SHOW_XML_SOURCE_FLAG = "showXMLSource"
        const val TOTAL_FACTOR = 5;
        const val MIN_IDLE_FACTOR = 0.5;
        const val MAX_IDLE_FACTOR = 0.75;
    }

    private val log = KotlinLogging.logger {}

    var devMode: Boolean  = false
	var poolsMaxPerKey: Int = 5
	var mediaType: MediaType? = null

	@Bean
	open fun getXSLTViewResolver(): ViewResolver {
		log.debug("Configuring SummerXSLTView");
		val xsltResolver = CustomXsltViewResolver()
        xsltResolver.setOrder(1)
		xsltResolver.setSourceKey(XML_SOURCE_TAG)
		xsltResolver.setViewClass(SummerXSLTView::class.java)
		xsltResolver.setViewNames("*" + XSLT_SUFFIX)
        xsltResolver.setPrefix("/WEB-INF/xslt/")
		return xsltResolver;
	}
}
