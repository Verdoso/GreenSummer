package org.greeneyed.summer.monitoring

import org.slf4j.Logger
import org.slf4j.MDC

import java.util.concurrent.LinkedBlockingQueue

class LoggingWorker(private val log: Logger) : Runnable {

    private val measuresQueue = LinkedBlockingQueue<ProfiledMeasure>()
    private var keepRunning = true

    override fun run() {
        if (log.isTraceEnabled) {
            log.trace("Monitoring logs enabled")
        }
        while (keepRunning) {
            try {
                val measure: ProfiledMeasure = measuresQueue.take()
                MDC.put(ProfiledMeasure.MDC_UUID_TOKEN_KEY, measure.token)
                var tags = ""
                if (measure.tags != null && measure.tags.isNotEmpty()) {
                    val tagsSB = StringBuilder("([")
                    for (count in 0..measure.tags.size - 1 - 1) {
                        tagsSB.append(measure.tags[count])
                        tagsSB.append(",")
                    }
                    tagsSB.append(measure.tags[measure.tags.size - 1])
                    tagsSB.append("])")
                    tags = tagsSB.toString()
                }
                if (measure.isShowValue) {
                    log.trace("{}{}(..) | {}", measure.name, tags, measure.value)
                } else {
                    log.trace("{}{}(..) | {}", measure.name, tags, "+1")
                }
            } catch (e: InterruptedException) {
                log.error("Blocking queue was interrupted {}", e)
            }

        }
    }

    fun signalToStop() {
        keepRunning = false
    }

    fun enqueue(message: String, token: String?, timeSpent: Long, tags: Array<String>?) {
        var logToken = token
        if (token == null) {
            logToken = MDC.get(ProfiledMeasure.MDC_UUID_TOKEN_KEY)
        }
        measuresQueue.add(ProfiledMeasure(message, timeSpent, true, tags, logToken))
    }

    fun enqueue(message: String, token: String?, tags: Array<String>?) {
        var logToken = token
        if (token == null) {
            logToken = MDC.get(ProfiledMeasure.MDC_UUID_TOKEN_KEY)
        }
        measuresQueue.add(ProfiledMeasure(message, 0, false, tags, logToken))
    }

    fun enqueue(profiledMeasure: ProfiledMeasure?) {
        if (profiledMeasure != null) {
            measuresQueue.add(profiledMeasure)
        }
    }
}
