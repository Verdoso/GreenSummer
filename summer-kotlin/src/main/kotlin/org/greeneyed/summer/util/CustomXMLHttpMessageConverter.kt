package org.greeneyed.summer.util

import java.io.IOException

import javax.xml.bind.JAXBElement
import javax.xml.bind.JAXBException
import javax.xml.bind.MarshalException
import javax.xml.bind.Marshaller
import javax.xml.bind.PropertyException
import javax.xml.bind.UnmarshalException
import javax.xml.bind.Unmarshaller
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.transform.Result
import javax.xml.transform.Source

import org.apache.commons.pool2.impl.GenericKeyedObjectPool
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig
import org.greeneyed.summer.config.XsltConfiguration
import org.greeneyed.summer.monitoring.Measured
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter
import org.springframework.util.ClassUtils

import mu.KotlinLogging

/**
 * The Class CustomXMLHttpMessageConverter.
 */
class CustomXMLHttpMessageConverter(poolsMaxPerKey: Int) : Jaxb2RootElementHttpMessageConverter() {

    private val log = KotlinLogging.logger {}
    private val unmarshallerPool: GenericKeyedObjectPool<Class<*>, Unmarshaller>
    private val marshallerPool: GenericKeyedObjectPool<Class<*>, Marshaller>

    init {
        val gop = GenericKeyedObjectPoolConfig()
        gop.maxTotal = poolsMaxPerKey * XsltConfiguration.TOTAL_FACTOR
        gop.minIdlePerKey = (poolsMaxPerKey / XsltConfiguration.MIN_IDLE_FACTOR).toInt()
        gop.maxIdlePerKey = (poolsMaxPerKey / XsltConfiguration.MAX_IDLE_FACTOR).toInt()
        gop.maxTotalPerKey = poolsMaxPerKey
        gop.testOnBorrow = false
        gop.maxWaitMillis = 10000
        log.info("Pool of unmarshallers initialised with concurrency {}", gop.maxTotalPerKey)
        unmarshallerPool = GenericKeyedObjectPool(UnmarshallerFactory(), gop)
        marshallerPool = GenericKeyedObjectPool(MarshallerFactory(), gop)
    }

    @Measured("parseXML")
    @Throws(IOException::class)
    override fun readFromSource(clazz: Class<*>, headers: HttpHeaders?, source: Source): Any {
        val result: Any
        var unmarshaller: Unmarshaller? = null
        try {
            val processedSource = processSource(source)
            unmarshaller = unmarshallerPool.borrowObject(clazz)
            result = processXMLRequest(clazz, unmarshaller, processedSource)
        } catch (ex: NullPointerException) {
            throw handleNPE(ex)
        } catch (ex: UnmarshalException) {
            throw HttpMessageNotReadableException("Could not unmarshal to [" + clazz + "]: " + ex.message, ex)
        } catch (ex: JAXBException) {
            throw HttpMessageConversionException("Could not instantiate JAXBContext: " + ex.message, ex)
        } catch (ex: Exception) {
            throw HttpMessageConversionException("Could not borrow unmarshaller from the pool: " + ex.message, ex)
        } finally {
            if (unmarshaller != null) {
                unmarshallerPool.returnObject(clazz, unmarshaller)
            }
        }
        return result
    }

    private fun handleNPE(ex: NullPointerException): HttpMessageNotReadableException {
        if (!isSupportDtd) {
            return HttpMessageNotReadableException("NPE while unmarshalling. " +
                    "This can happen on JDK 1.6 due to the presence of DTD " +
                    "declarations, which are disabled.", ex)
        } else {
            throw ex
        }
    }

    @Measured("createXML")
    @Throws(IOException::class)
    override fun writeToResult(o: Any, headers: HttpHeaders, result: Result) {
        var marshaller: Marshaller? = null
        var clazz: Class<*>? = null
        try {
            clazz = ClassUtils.getUserClass(o)
            marshaller = marshallerPool.borrowObject(clazz)
            setCharset(headers.contentType, marshaller)
            marshaller!!.marshal(o, result)
        } catch (ex: MarshalException) {
            throw HttpMessageNotWritableException("Could not marshal [" + o + "]: " + ex.message, ex)
        } catch (ex: JAXBException) {
            throw HttpMessageConversionException("Could not instantiate JAXBContext: " + ex.message, ex)
        } catch (ex: Exception) {
            throw HttpMessageConversionException("Could not borrow marshaller from the pool: " + ex.message, ex)
        } finally {
            if (clazz != null && marshaller != null) {
                marshallerPool.returnObject(clazz, marshaller)
            }
        }
    }

    @Throws(JAXBException::class)
    private fun processXMLRequest(clazz: Class<*>, unmarshaller: Unmarshaller, processedSource: Source): Any {
        val result: Any
        if (clazz.isAnnotationPresent(XmlRootElement::class.java)) {
            result = unmarshaller.unmarshal(processedSource)
        } else {
            result = unmarshaller.unmarshal(processedSource, clazz).value
        }
        return result
    }

    @Throws(PropertyException::class)
    private fun setCharset(contentType: MediaType?, marshaller: Marshaller) {
        if (contentType != null && contentType.charset != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, contentType.charset.name())
        }
    }
}
