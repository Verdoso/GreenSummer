package org.greeneyed.summer.util

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory
import org.apache.commons.pool2.PooledObject
import org.apache.commons.pool2.impl.DefaultPooledObject
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller

class MarshallerFactory : BaseKeyedPooledObjectFactory<Class<*>, Marshaller>() {
    companion object {
        private val JAXB_CONTEXT_MAP = HashMap<Class<*>, JAXBContext>()
    }
    override fun create(clazz: Class<*>): Marshaller? = JAXB_CONTEXT_MAP.getOrPut(clazz,{JAXBContext.newInstance(clazz)}).createMarshaller()
    override fun wrap(wrapper: Marshaller): PooledObject<Marshaller> = DefaultPooledObject(wrapper)
}