package org.greeneyed.summer.util

import mu.KotlinLogging
import org.apache.commons.pool2.impl.GenericKeyedObjectPool
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig
import org.greeneyed.summer.config.XsltConfiguration
import org.greeneyed.summer.monitoring.Measured
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextException
import org.springframework.context.MessageSource
import org.springframework.context.MessageSourceAware
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import org.springframework.web.servlet.view.xslt.XsltView
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.xml.bind.JAXBException
import javax.xml.bind.MarshalException
import javax.xml.bind.Marshaller
import javax.xml.bind.PropertyException
import javax.xml.bind.util.JAXBSource
import javax.xml.transform.Source
import javax.xml.transform.Templates
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.stream.StreamSource

@Component
class SummerXSLTView : XsltView(), MessageSourceAware {

    private val log = KotlinLogging.logger {}

    private var marshallerPool: GenericKeyedObjectPool<Class<*>, Marshaller>? = null
    private var mediaType: MediaType? = null
    private var messageSource: MessageSource? = null
    private var devMode = false

    override fun setMessageSource(messageSource: MessageSource) {
        this.messageSource = messageSource
        if (this.messageSource is ApplicationContext && marshallerPool == null) {
            val xsltConfiguration = (this.messageSource as ApplicationContext)
                    .getBean(XsltConfiguration::class.java)
            if (xsltConfiguration != null) {
                mediaType = xsltConfiguration.mediaType
                devMode = xsltConfiguration.devMode
                val gop = GenericKeyedObjectPoolConfig()
                gop.maxTotal = xsltConfiguration.poolsMaxPerKey * XsltConfiguration.TOTAL_FACTOR
                gop.minIdlePerKey = (xsltConfiguration.poolsMaxPerKey / XsltConfiguration.MIN_IDLE_FACTOR).toInt()
                gop.maxIdlePerKey = (xsltConfiguration.poolsMaxPerKey / XsltConfiguration.MAX_IDLE_FACTOR).toInt()
                gop.maxTotalPerKey = xsltConfiguration.poolsMaxPerKey
                gop.testOnBorrow = false
                gop.maxWaitMillis = 10000
                log.info("Pool of marshallers initialised with concurrency {}", gop.maxTotalPerKey)
                marshallerPool = GenericKeyedObjectPool(MarshallerFactory(), gop)
            } else {
                throw IllegalArgumentException("No XsltConfiguration bean found!")
            }
        }
    }

    @Measured("generateXML")
    @Throws(Exception::class)
    override fun convertSource(source: Any): Source {
        if (!(source is Source || source is Document || source is Node
                || source is Reader || source is InputStream || source is Resource)) {
            var marshaller: Marshaller? = null
            var clazz: Class<*>? = null
            try {
                clazz = ClassUtils.getUserClass(source)
                marshaller = marshallerPool!!.borrowObject(clazz)
                setCharset(marshaller)
                return super.convertSource(JAXBSource(marshaller!!, source))
            } catch (ex: MarshalException) {
                throw HttpMessageNotWritableException("Could not marshal [" + source + "]: " + ex.message, ex)
            } catch (ex: JAXBException) {
                throw HttpMessageConversionException("Could not instantiate JAXBContext: " + ex.message, ex)
            } catch (ex: Exception) {
                throw HttpMessageConversionException(
                        "Could not borrow marshaller from the pool: " + ex.message, ex)
            } finally {
                if (clazz != null && marshaller != null) {
                    marshallerPool!!.returnObject(clazz, marshaller)
                }
            }
        } else {
            return super.convertSource(source)
        }
    }

    @Measured("xsltTransform")
    @Throws(Exception::class)
    override fun renderMergedOutputModel(model: Map<String, Any>
                                         , request: HttpServletRequest
                                         , response: HttpServletResponse) {
        val transformer = getTransformer(model, request)
        if (transformer != null) {
            var source: Source? = null
            try {
                source = locateSource(model)
                if (source == null) {
                    throw IllegalArgumentException("Unable to locate Source object in model: " + model)
                }
                transformer.transform(source, createResult(response))
            } finally {
                closeSourceIfNecessary(source)
            }
        } else {
            super.renderMergedOutputModel(model, request, response)
        }
    }

    @Throws(TransformerConfigurationException::class)
    private fun getTransformer(model: Map<String, Any>, request: HttpServletRequest): Transformer? {
        var transformer: Transformer? = null
        var showXML = java.lang.Boolean.TRUE == model[XsltConfiguration.SHOW_XML_SOURCE_FLAG]
        var refreshXSLT = devMode || java.lang.Boolean.TRUE == model[XsltConfiguration.REFRESH_XSLT_FLAG]
        if (!showXML && devMode) {
            var showXMLString: String? = request.getParameter(XsltConfiguration.SHOW_XML_SOURCE_FLAG)
            if (showXMLString == null) {
                showXMLString = request.getAttribute(XsltConfiguration.SHOW_XML_SOURCE_FLAG) as String?
            }
            showXML = java.lang.Boolean.parseBoolean(showXMLString)
        }
        if (!refreshXSLT) {
            var refreshXSLTString: String? = request.getParameter(XsltConfiguration.REFRESH_XSLT_FLAG)
            if (refreshXSLTString == null) {
                refreshXSLTString = request.getAttribute(XsltConfiguration.REFRESH_XSLT_FLAG) as String?
            }
            refreshXSLT = java.lang.Boolean.parseBoolean(refreshXSLTString)
        }
        if (showXML) {
            transformer = transformerFactory.newTransformer()
        } else if (refreshXSLT) {
            transformer = createTransformer(loadTemplates())
        }
        return transformer
    }

    /**
     * Copied due to private access so no reusing from children class :(. Load
     * the [Templates] instance for the stylesheet at the configured
     * location.
     */
    @Throws(ApplicationContextException::class)
    private fun loadTemplates(): Templates {
        val stylesheetSource = stylesheetSource
        try {
            val templates = transformerFactory.newTemplates(stylesheetSource)
            if (logger.isDebugEnabled) {
                logger.debug("Loading templates '$templates'")
            }
            return templates
        } catch (ex: TransformerConfigurationException) {
            throw ApplicationContextException("Can't load stylesheet from '$url'", ex)
        } finally {
            closeSourceIfNecessary(stylesheetSource)
        }
    }

    /**
     * Copied due to private access so no reusing from children class :(. Close
     * the underlying resource managed by the supplied [Source] if
     * applicable.
     *
     *
     * Only works for [StreamSources][StreamSource].

     * @param source
     * *            the XSLT Source to close (may be `null`)
     */
    private fun closeSourceIfNecessary(source: Source?) {
        if (source is StreamSource) {
            val streamSource = source
            if (streamSource.reader != null) {
                try {
                    streamSource.reader.close()
                } catch (ex: IOException) {
                    // ignore
                }

            }
            if (streamSource.inputStream != null) {
                try {
                    streamSource.inputStream.close()
                } catch (ex: IOException) {
                    // ignore
                }
            }
        }
    }

    @Throws(PropertyException::class)
    private fun setCharset(marshaller: Marshaller) {
        if (mediaType != null && mediaType!!.charset != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, mediaType!!.charset.name())
        }
    }
}
