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


import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.status.ErrorStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public final class LogbackMemoryAppenderImpl extends AppenderBase<ILoggingEvent> {

    private final Deque<String> registeredEvents = new ConcurrentLinkedDeque<>();

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();

    private final int size;
    private final String pattern;
    // Set up the pattern
    private PatternLayout patternLayout = new PatternLayout();

    @Override
    protected void append(ILoggingEvent eventObject) {
        readLock.lock();
        try {
            String event = patternLayout.doLayout(eventObject);
            registeredEvents.addFirst(event);
            if (registeredEvents.size() > size) {
                registeredEvents.removeLast();
            }
        } catch (Exception e) {
            addStatus(new ErrorStatus("Failed to store event [" + name + "].", this, e));
        } finally {
            readLock.unlock();
        }

    }

    @Override
    public void start() {
        super.start();
        patternLayout.setContext(getContext());
        patternLayout.setPattern(pattern);
        patternLayout.start();
    }

    @Override
    public void stop() {
        super.stop();
        readLock.lock();
        try {
            registeredEvents.clear();
        } finally {
            readLock.unlock();
        }
    }
}
