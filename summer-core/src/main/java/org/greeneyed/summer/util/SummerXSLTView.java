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
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.greeneyed.summer.config.XsltConfiguration;
import org.greeneyed.summer.monitoring.Measured;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.view.xslt.XsltView;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
@Component
public class SummerXSLTView extends XsltView implements MessageSourceAware {

    private GenericKeyedObjectPool<Class<?>, Marshaller> marshallerPool = null;
    private MediaType mediaType;
    private MessageSource messageSource;
    private boolean devMode = false;

    @Override
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
        if (this.messageSource instanceof ApplicationContext) {
            XsltConfiguration xsltConfiguration = ((ApplicationContext) this.messageSource).getBean(XsltConfiguration.class);
            if (xsltConfiguration != null) {
                mediaType = xsltConfiguration.getMediaType();
                devMode = xsltConfiguration.isDevMode();
                GenericKeyedObjectPoolConfig gop = new GenericKeyedObjectPoolConfig();
                gop.setMaxTotal(xsltConfiguration.getPoolsMaxPerKey() * XsltConfiguration.TOTAL_FACTOR);
                gop.setMinIdlePerKey((int) (xsltConfiguration.getPoolsMaxPerKey() / XsltConfiguration.MIN_IDLE_FACTOR));
                gop.setMaxIdlePerKey((int) (xsltConfiguration.getPoolsMaxPerKey() / XsltConfiguration.MAX_IDLE_FACTOR));
                gop.setMaxTotalPerKey(xsltConfiguration.getPoolsMaxPerKey());
                gop.setTestOnBorrow(false);
                gop.setMaxWaitMillis(10_000);
                log.info("Pool of unmarshallers initialised with concurrency {}", gop.getMaxTotalPerKey());
                marshallerPool = new GenericKeyedObjectPool<>(new MarshallerFactory(), gop);
            } else {
                throw new IllegalArgumentException("No XsltConfiguration bean found!");
            }
        }
    }

    @Override
    @Measured("generateXML")
    protected Source convertSource(Object source) throws Exception {
        if (!(source instanceof Source || source instanceof Document || source instanceof Node || source instanceof Reader
            || source instanceof InputStream || source instanceof Resource)) {
            Marshaller marshaller = null;
            Class<?> clazz = null;
            try {
                clazz = ClassUtils.getUserClass(source);
                marshaller = marshallerPool.borrowObject(clazz);
                setCharset(marshaller);
                return super.convertSource(new JAXBSource(marshaller, source));
            } catch (MarshalException ex) {
                throw new HttpMessageNotWritableException("Could not marshal [" + source + "]: " + ex.getMessage(), ex);
            } catch (JAXBException ex) {
                throw new HttpMessageConversionException("Could not instantiate JAXBContext: " + ex.getMessage(), ex);
            } catch (Exception ex) {
                throw new HttpMessageConversionException("Could not borrow marshaller from the pool: " + ex.getMessage(), ex);
            } finally {
                if (clazz != null && marshaller != null) {
                    marshallerPool.returnObject(clazz, marshaller);
                }
            }
        } else {
            return super.convertSource(source);
        }
    }

    @Override
    @Measured("xsltTransform")
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Transformer transformer = getTransformer(model, request);
        if (transformer != null) {
            Source source = null;
            try {
                source = locateSource(model);
                if (source == null) {
                    throw new IllegalArgumentException("Unable to locate Source object in model: " + model);
                }
                transformer.transform(source, createResult(response));
            } finally {
                closeSourceIfNecessary(source);
            }
        } else {
            super.renderMergedOutputModel(model, request, response);
        }
    }

    private Transformer getTransformer(Map<String, Object> model, HttpServletRequest request) throws TransformerConfigurationException {
        Transformer transformer = null;
        boolean showXML = Boolean.TRUE.equals(model.get(XsltConfiguration.SHOW_XML_SOURCE_FLAG));
        boolean refreshXSLT = devMode || Boolean.TRUE.equals(model.get(XsltConfiguration.REFRESH_XSLT_FLAG));
        if (!showXML && devMode) {
            String showXMLString = request.getParameter(XsltConfiguration.SHOW_XML_SOURCE_FLAG);
            if (showXMLString == null) {
                showXMLString = (String) request.getAttribute(XsltConfiguration.SHOW_XML_SOURCE_FLAG);
            }
            showXML = Boolean.parseBoolean(showXMLString);
        }
        if (!refreshXSLT) {
            String refreshXSLTString = request.getParameter(XsltConfiguration.REFRESH_XSLT_FLAG);
            if (refreshXSLTString == null) {
                refreshXSLTString = (String) request.getAttribute(XsltConfiguration.REFRESH_XSLT_FLAG);
            }
            refreshXSLT = Boolean.parseBoolean(refreshXSLTString);
        }
        if (showXML) {
            transformer = getTransformerFactory().newTransformer();
        } else if (refreshXSLT) {
            transformer = createTransformer(loadTemplates());
        }
        return transformer;
    }

    /**
     * Copied due to private access so no reusing from children class :(. Load
     * the {@link Templates} instance for the stylesheet at the configured
     * location.
     */
    private Templates loadTemplates() throws ApplicationContextException {
        Source stylesheetSource = getStylesheetSource();
        try {
            Templates templates = getTransformerFactory().newTemplates(stylesheetSource);
            if (logger.isDebugEnabled()) {
                logger.debug("Loading templates '" + templates + "'");
            }
            return templates;
        } catch (TransformerConfigurationException ex) {
            throw new ApplicationContextException("Can't load stylesheet from '" + getUrl() + "'", ex);
        } finally {
            closeSourceIfNecessary(stylesheetSource);
        }
    }

    /**
     * Copied due to private access so no reusing from children class :(. Close
     * the underlying resource managed by the supplied {@link Source} if
     * applicable.
     * <p>
     * Only works for {@link StreamSource StreamSources}.
     * 
     * @param source
     *        the XSLT Source to close (may be {@code null})
     */
    private void closeSourceIfNecessary(Source source) {
        if (source instanceof StreamSource) {
            StreamSource streamSource = (StreamSource) source;
            if (streamSource.getReader() != null) {
                try {
                    streamSource.getReader().close();
                } catch (IOException ex) {
                    // ignore
                }
            }
            if (streamSource.getInputStream() != null) {
                try {
                    streamSource.getInputStream().close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    private void setCharset(Marshaller marshaller) throws PropertyException {
        if (mediaType != null && mediaType.getCharset() != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, mediaType.getCharset().name());
        }
    }
}