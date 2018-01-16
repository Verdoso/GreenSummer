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


import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import lombok.extern.slf4j.Slf4j;

/**
 * A factory for creating Unmarshaller objects.
 */
@Slf4j
public class UnmarshallerFactory extends BaseKeyedPooledObjectFactory<Class<?>, Unmarshaller> {

    private static final Map<Class<?>, JAXBContext> JAXB_CONTEXT_MAP = new HashMap<>();

    @Override
    public Unmarshaller create(Class<?> clazz) {
        // Retrieve or create a JACBContext for this key
        JAXBContext jc = JAXB_CONTEXT_MAP.get(clazz);
        if (jc == null) {
            try {
                jc = JAXBContext.newInstance(clazz);
                // JAXB Context is threadsafe, it can be reused, so let's store it for later
                JAXB_CONTEXT_MAP.put(clazz, jc);
            } catch (JAXBException e) {
                log.error("Error creating JAXBContext", e);
                return null;
            }
        }
        try {
            return jc.createUnmarshaller();
        } catch (JAXBException e) {
            log.error("Error creating Unmarshaller", e);
            return null;
        }
    }

    @Override
    public PooledObject<Unmarshaller> wrap(Unmarshaller wrapper) {
        return new DefaultPooledObject<>(wrapper);
    }
}
