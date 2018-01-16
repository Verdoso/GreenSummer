package org.greeneyed.summer.monitoring

import mu.KotlinLogging
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.greeneyed.summer.filter.Log4jMDCFilter
import org.springframework.beans.factory.annotation.Value

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import java.util.function.Supplier

@Aspect
abstract class ProfilingAspect {
    private val log = KotlinLogging.logger {}

    private var threshold: Long = 10
    private var configuredTags: Array<String>? = null
    private val loggingWorker: LoggingWorker = LoggingWorker(log)
    private val loggingWorkerThread: Thread = Thread(loggingWorker)
    private val requestFilter: Predicate<HttpServletRequest> = getRequestFilter()

    @Value("\${spring.application.name}")
    private val serviceName: String? = null

    fun configure(threshold: Long, tags: Array<String>?) {
        this.threshold = threshold
        configuredTags = tags
        loggingWorkerThread.isDaemon = true
        loggingWorkerThread.start()
        log.info("Profiling aspect started with tags {} and threshold {}", configuredTags, threshold)
    }

    open protected fun getRequestFilter(): Predicate<HttpServletRequest> {
        return Predicate{ r: HttpServletRequest -> r.requestURI != null }
    }

    fun stopProfiling() {
        loggingWorker.signalToStop()
        loggingWorkerThread.interrupt()
    }

    @Pointcut("@annotation(org.greeneyed.summer.monitoring.Measured)")
    fun measuredMethods() {
        // Pointcut methods need no body
    }

    @Pointcut("@annotation(org.greeneyed.summer.monitoring.Counted)")
    fun countedMethods() {
        // Pointcut methods need no body
    }

    @Pointcut("execution(@org.springframework.web.bind.annotation.RequestMapping * *(..))")
    fun requestMappings() {
        // Pointcut methods need no body
    }

    /**
     * XML Processing methods.
     */
    @Pointcut("execution(* org.eclipse.jetty.servlet.ServletHandler.doHandle(..))")
    fun servletHandlerHandle() {
        // Pointcut methods need no body
    }

    @Around("servletHandlerHandle()")
    @Throws(Throwable::class)
    fun profileJettyContainer(proceedingJoinPoint: ProceedingJoinPoint): Any? {
        val request = proceedingJoinPoint.args[2] as HttpServletRequest
        val response = proceedingJoinPoint.args[3] as HttpServletResponse
        if (requestFilter.test(request)) {
            return measure(proceedingJoinPoint, buildNameForRequest(request),
                    { response.getHeader(Log4jMDCFilter.RESPONSE_TOKEN_HEADER) })
        } else {
            return proceedingJoinPoint.proceed()
        }
    }

    open protected fun buildNameForRequest(request: HttpServletRequest): String {
        var name = request.requestURI.substring(1)
        val nextSlash = name.indexOf('/', 1)
        if (nextSlash > -1) {
            name = name.substring(0, nextSlash)
        }
        return "Container.service." + name + "_" + request.method
    }

    @Throws(Throwable::class)
    @JvmOverloads fun measure(proceedingJoinPoint: ProceedingJoinPoint, name: String = getMessageString(proceedingJoinPoint), tokenExtractor: (() -> String)? = null): Any? {
        val startTime = System.nanoTime()
        try {
            return proceedingJoinPoint.proceed()
        } finally {
            val timeSpent = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
            if (timeSpent >= threshold) {
                time(name = name, token = tokenExtractor?.invoke(), value = timeSpent)
            }
        }
    }

    @Throws(Throwable::class)
    @JvmOverloads fun count(proceedingJoinPoint: ProceedingJoinPoint, tokenExtractor: Supplier<String>? = null): Any {
        try {
            return proceedingJoinPoint.proceed()
        } finally {
            val name = getMessage(proceedingJoinPoint).toString()
            count(name = name, token = tokenExtractor?.get())
        }
    }

    protected fun processMeasure(measure: ProfiledMeasure) {
        if (log.isTraceEnabled) {
            loggingWorker.enqueue(measure)
        }
    }

    private fun joinTags(tags: Array<String>?, extraTags: Array<String>?): Array<String>? {
        val result: Array<String>?
        if (tags == null) {
            result = extraTags
        } else if (extraTags == null) {
            result = tags
        } else {
            result = Array(tags.size + extraTags.size) { "" }
            System.arraycopy(tags, 0, result, 0, tags.size)
            System.arraycopy(extraTags, 0, result, tags.size, extraTags.size)
        }
        return result
    }

    protected fun count(name: String, token: String?, tags: Array<String>? = null) = processMeasure(ProfiledMeasure(name = name, value = 1L, tags = joinTags(configuredTags, tags), token = token))

    protected fun time(name: String, token: String?, value: Long, tags: Array<String>? = null) = processMeasure(ProfiledMeasure(name = name, value = value, tags = joinTags(configuredTags, tags), token = token, isShowValue = true))

    protected fun getMessageString(joinPoint: JoinPoint): String  = getMessage(joinPoint).toString()

    open protected fun getMessage(joinPoint: JoinPoint): StringBuilder {
        val messageSB = StringBuilder()
        if (joinPoint.target != null) {
            messageSB.append(joinPoint.target.javaClass.simpleName)
        } else {
            messageSB.append(joinPoint.signature.declaringType.simpleName)
        }
        if (joinPoint.signature != null) {
            messageSB.append('.')
            var name = joinPoint.signature.name
            if (joinPoint.signature is MethodSignature) {
                val methodSignature = joinPoint.signature as MethodSignature
                val targetMethod = methodSignature.method
                val measured : Measured? = targetMethod.getAnnotation(Measured::class.java)
                if (measured != null && measured.value.trim { it <= ' ' }.length > 0) {
                    name = measured.value
                } else {
                    val counted : Counted? = targetMethod.getAnnotation(Counted::class.java)
                    if (counted != null && counted.value.trim { it <= ' ' }.length > 0) {
                        name = counted.value
                    }
                }
            }
            messageSB.append(name)
        }
        return messageSB
    }
}
