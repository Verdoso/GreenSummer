package org.greeneyed.summer.util

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory
import org.apache.commons.pool2.PooledObject
import org.apache.commons.pool2.impl.DefaultPooledObject
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.bind.Unmarshaller

class UnmarshallerFactory : BaseKeyedPooledObjectFactory<Class<*>, Unmarshaller>() {

    companion object {
        private val JAXB_CONTEXT_MAP = HashMap<Class<*>, JAXBContext>()
    }
    override fun create(clazz: Class<*>): Unmarshaller = JAXB_CONTEXT_MAP.getOrPut(clazz,{JAXBContext.newInstance(clazz)}).createUnmarshaller()
    override fun wrap(wrapper: Unmarshaller): PooledObject<Unmarshaller> = DefaultPooledObject(wrapper)
}