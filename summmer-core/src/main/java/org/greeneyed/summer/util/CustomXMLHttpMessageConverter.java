package org.greeneyed.summer.util;

/*
 * #%L
 * Summer
 * %%
 * Copyright (C) 2018 GreenEyed (Daniel Lopez)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.greeneyed.summer.config.XsltConfiguration;
import org.greeneyed.summer.monitoring.Measured;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.util.ClassUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * The Class CustomXMLHttpMessageConverter.
 */
@Slf4j
public class CustomXMLHttpMessageConverter extends Jaxb2RootElementHttpMessageConverter {

    private final GenericKeyedObjectPool<Class<?>, Unmarshaller> unmarshallerPool;
    private final GenericKeyedObjectPool<Class<?>, Marshaller> marshallerPool;

    public CustomXMLHttpMessageConverter(final int poolsMaxPerKey) {
        final GenericKeyedObjectPoolConfig gop = new GenericKeyedObjectPoolConfig();
        gop.setMaxTotal(poolsMaxPerKey * XsltConfiguration.TOTAL_FACTOR);
        gop.setMinIdlePerKey((int) (poolsMaxPerKey / XsltConfiguration.MIN_IDLE_FACTOR));
        gop.setMaxIdlePerKey((int) (poolsMaxPerKey / XsltConfiguration.MAX_IDLE_FACTOR));
        gop.setMaxTotalPerKey(poolsMaxPerKey);
        gop.setTestOnBorrow(false);
        gop.setMaxWaitMillis(10_000);
        log.info("Pool of unmarshallers initialised with concurrency {}", gop.getMaxTotalPerKey());
        unmarshallerPool = new GenericKeyedObjectPool<>(new UnmarshallerFactory(), gop);
        marshallerPool = new GenericKeyedObjectPool<>(new MarshallerFactory(), gop);
    }

    @Override
    @Measured("parseXML")
    protected Object readFromSource(Class<?> clazz, HttpHeaders headers, Source source) throws IOException {
        final Object result;
        Unmarshaller unmarshaller = null;
        try {
            final Source processedSource = processSource(source);
            unmarshaller = unmarshallerPool.borrowObject(clazz);
            result = processXMLRequest(clazz, unmarshaller, processedSource);
        } catch (NullPointerException ex) {
            throw handleNPE(ex);
        } catch (UnmarshalException ex) {
            throw new HttpMessageNotReadableException("Could not unmarshal to [" + clazz + "]: " + ex.getMessage(), ex);
        } catch (JAXBException ex) {
            throw new HttpMessageConversionException("Could not instantiate JAXBContext: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new HttpMessageConversionException("Could not borrow unmarshaller from the pool: " + ex.getMessage(), ex);
        } finally {
            if (unmarshaller != null) {
                unmarshallerPool.returnObject(clazz, unmarshaller);
            }
        }
        return result;
    }

    private HttpMessageNotReadableException handleNPE(NullPointerException ex) {
        if (!isSupportDtd()) {
            return new HttpMessageNotReadableException("NPE while unmarshalling. " + "This can happen on JDK 1.6 due to the presence of DTD "
                + "declarations, which are disabled.", ex);
        } else {
            throw ex;
        }
    }

    @Override
    @Measured("createXML")
    protected void writeToResult(Object o, HttpHeaders headers, Result result) throws IOException {
        Marshaller marshaller = null;
        Class<?> clazz = null;
        try {
            clazz = ClassUtils.getUserClass(o);
            marshaller = marshallerPool.borrowObject(clazz);
            setCharset(headers.getContentType(), marshaller);
            marshaller.marshal(o, result);
        } catch (MarshalException ex) {
            throw new HttpMessageNotWritableException("Could not marshal [" + o + "]: " + ex.getMessage(), ex);
        } catch (JAXBException ex) {
            throw new HttpMessageConversionException("Could not instantiate JAXBContext: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new HttpMessageConversionException("Could not borrow marshaller from the pool: " + ex.getMessage(), ex);
        } finally {
            if (clazz != null && marshaller != null) {
                marshallerPool.returnObject(clazz, marshaller);
            }
        }
    }

    private Object processXMLRequest(Class<?> clazz, Unmarshaller unmarshaller, final Source processedSource) throws JAXBException {
        final Object result;
        if (clazz.isAnnotationPresent(XmlRootElement.class)) {
            result = unmarshaller.unmarshal(processedSource);
        } else {
            JAXBElement<?> jaxbElement = unmarshaller.unmarshal(processedSource, clazz);
            result = jaxbElement.getValue();
        }
        return result;
    }

    private void setCharset(MediaType contentType, Marshaller marshaller) throws PropertyException {
        if (contentType != null && contentType.getCharset() != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, contentType.getCharset().name());
        }
    }
}
