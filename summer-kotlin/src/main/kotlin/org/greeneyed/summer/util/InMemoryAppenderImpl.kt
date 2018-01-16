package org.greeneyed.summer.util;

import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.appender.AppenderLoggingException
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.PatternLayout
import java.io.Serializable
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

// note: class name need not match the @Plugin name.
@Plugin(name = "InMemoryAppender", category = "Core", elementType = "appender", printObject = true)
class InMemoryAppenderImpl(name: String, filter: Filter?, layout: Layout<out Serializable>?, ignoreExceptions: Boolean, var size: Int) : AbstractAppender(name, filter, layout, ignoreExceptions) {

    companion object {
        // Your custom appender needs to declare a factory method
        // annotated with `@PluginFactory`. Log4j will parse the configuration
        // and call this factory method to construct an appender instance with
        /**
         * Creates the appender.

         * @param name the name
         * *
         * @param layout the layout
         * *
         * @param filter the filter
         * *
         * @param size the size
         * *
         * @return the in memory appender impl
         */
        // the configured attributes.
        @PluginFactory
        fun createAppender(
                @PluginAttribute("name") name: String?,
                @PluginElement("Layout") layout: Layout<out Serializable>?,
                @PluginElement("Filter") filter: Filter?,
                @PluginAttribute("size") size: Int): InMemoryAppenderImpl? {
            if (name == null) {
                LOGGER.error("No name provided for InMemoryAppenderImpl")
                return null
            }
            var layoutAux: Layout<out Serializable>
            if (layout == null) {
                layoutAux = PatternLayout.createDefaultLayout() as Layout<out Serializable>
            }
            else {
                layoutAux = layout
            }
            return InMemoryAppenderImpl(name, filter, layoutAux, true, size)
        }
    }


    val registeredEvents: Deque<String> = ConcurrentLinkedDeque<String>()
    val rwLock: ReadWriteLock = ReentrantReadWriteLock()
    val readLock: Lock = rwLock.readLock()

    //private final int size;

    // The append method is where the appender does the work.
    // Given a log event, you are free to do with it what you want.
    // This example demonstrates:
    // 1. Concurrency: this method may be called by multiple threads concurrently
    // 2. How to use layouts
    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.Appender#append(org.apache.logging.log4j.core.LogEvent)
     */
    // 3. Error handling
    override fun append(event: LogEvent) {
        readLock.lock()
        try {
            val bytes = layout.toByteArray(event)
            registeredEvents.addFirst(String(bytes))
            if (registeredEvents.size > size) {
                registeredEvents.removeLast()
            }
        } catch (ex: Exception) {
            if (!ignoreExceptions()) {
                throw AppenderLoggingException(ex)
            }
        } finally {
            readLock.unlock()
        }
    }
}
