package org.greeneyed.summer.monitoring;

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


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.MDC;

public class LoggingWorker implements Runnable {

    private final BlockingQueue<ProfiledMeasure> measuresQueue = new LinkedBlockingQueue<ProfiledMeasure>();
    private boolean keepRunning = true;
    private final Logger log;

    public LoggingWorker(Logger log) {
        this.log = log;
    }

    @Override
    public void run() {
        if (log.isTraceEnabled()) {
            log.trace("Monitoring logs enabled");
        }
        while (keepRunning) {
            try {
                final ProfiledMeasure measure = measuresQueue.take();
                MDC.put(ProfiledMeasure.MDC_UUID_TOKEN_KEY, measure.getToken());
                String tags = "";
                if (measure.getTags() != null && measure.getTags().length > 0) {
                    StringBuilder tagsSB = new StringBuilder("([");
                    for (int count = 0; count < measure.getTags().length - 1; count++) {
                        tagsSB.append(measure.getTags()[count]);
                        tagsSB.append(",");
                    }
                    tagsSB.append(measure.getTags()[measure.getTags().length - 1]);
                    tagsSB.append("])");
                    tags = tagsSB.toString();
                }
                if (measure.isShowValue()) {
                    log.trace("{}{}(..) | {}", measure.getName(), tags, measure.getValue());
                } else {
                    log.trace("{}{}(..) | {}", measure.getName(), tags, "+1");
                }
            } catch (final InterruptedException e) {
                log.error("Blocking queue was interrupted {}", e);
            }
        }
    }

    public void signalToStop() {
        keepRunning = false;
    }

    public void enqueue(final String message, String token, final long timeSpent, String... tags) {
        if (token == null) {
            token = MDC.get(ProfiledMeasure.MDC_UUID_TOKEN_KEY);
        }
        measuresQueue.add(new ProfiledMeasure(message, timeSpent, true, tags, token));
    }

    public void enqueue(final String message, String token, String... tags) {
        if (token == null) {
            token = MDC.get(ProfiledMeasure.MDC_UUID_TOKEN_KEY);
        }
        measuresQueue.add(new ProfiledMeasure(message, 0, false, tags, token));
    }

    public void enqueue(final ProfiledMeasure profiledMeasure) {
        if (profiledMeasure != null) {
            measuresQueue.add(profiledMeasure);
        }
    }
}
