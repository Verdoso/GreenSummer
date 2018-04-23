package org.greeneyed.summer.util.logging;

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


import java.io.Serializable;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

import lombok.Data;
import lombok.EqualsAndHashCode;

// note: class name need not match the @Plugin name.
@Plugin(name = "InMemoryAppender", category = "Core", elementType = "appender", printObject = true)
@Data
@EqualsAndHashCode(callSuper = false)
public final class InMemoryAppenderImpl extends AbstractAppender {

    private final Deque<String> registeredEvents = new ConcurrentLinkedDeque<>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();

    private final int size;

    protected InMemoryAppenderImpl(String name, Filter filter, Layout<? extends Serializable> layout, final boolean ignoreExceptions, final int size) {
        super(name, filter, layout, ignoreExceptions);
        this.size = size;
    }

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
    @Override
    public void append(LogEvent event) {
        readLock.lock();
        try {
            final byte[] bytes = getLayout().toByteArray(event);
            registeredEvents.addFirst(new String(bytes));
            if (registeredEvents.size() > size) {
                registeredEvents.removeLast();
            }
        } catch (Exception ex) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(ex);
            }
        } finally {
            readLock.unlock();
        }
    }

    // Your custom appender needs to declare a factory method
    // annotated with `@PluginFactory`. Log4j will parse the configuration
    // and call this factory method to construct an appender instance with
    /**
     * Creates the appender.
     *
     * @param name the name
     * @param layout the layout
     * @param filter the filter
     * @param size the size
     * @return the in memory appender impl
     */
    // the configured attributes.
    @PluginFactory
    public static InMemoryAppenderImpl createAppender(@PluginAttribute("name") String name,
        @PluginElement("Layout") Layout<? extends Serializable> layout, @PluginElement("Filter")
        final Filter filter, @PluginAttribute("size") int size) {
        Layout<? extends Serializable> layoutAux = layout;
        if (name == null) {
            LOGGER.error("No name provided for InMemoryAppenderImpl");
            return null;
        }
        if (layoutAux == null) {
            layoutAux = PatternLayout.createDefaultLayout();
        }
        return new InMemoryAppenderImpl(name, filter, layoutAux, true, size);
    }
}
