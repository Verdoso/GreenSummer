package org.greeneyed.summer.controller;

import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.AppenderRef
import org.apache.logging.log4j.core.config.LoggerConfig
import org.greeneyed.summer.messages.LogResponse
import org.greeneyed.summer.messages.LogSpecification
import org.greeneyed.summer.util.InMemoryAppenderImpl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.stream.Collectors
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletResponse


/**
 * The Class Log4JController.
 */
@Controller
@RequestMapping(value = ["/log4j"])
class Log4JController {

    private var inMemoryAppenderImpl: InMemoryAppenderImpl? = null


    private val log = KotlinLogging.logger {}

    /**
     * Setup.
     */
    @PostConstruct
    fun setup() {
        initInMemoryAppender()
    }

    private fun initInMemoryAppender() {
        val ctx = LogManager.getContext(false) as LoggerContext
        synchronized(ctx) {
            inMemoryAppenderImpl = null
            val config = ctx.configuration
            val baseAppender = config.appenders.values.stream().findFirst()

            if (baseAppender.isPresent) {
                inMemoryAppenderImpl = InMemoryAppenderImpl.createAppender("InMemoryAppenderImplAppender",
                        baseAppender.get().layout,
                        config.filter,
                        200)
                inMemoryAppenderImpl!!.start()
                config.addAppender(inMemoryAppenderImpl)
            }
        }
    }

    /**
     * List.

     * @return the response entity
     */
    @RequestMapping(value = ["list"], produces = [(MediaType.APPLICATION_JSON_VALUE)], method = [(RequestMethod.GET)], headers = ["Accept=application/json"])
    @ResponseBody
    fun list(): ResponseEntity<LogResponse> {
        val ctx = LogManager.getContext(false) as LoggerContext
        return ResponseEntity(listLoggers(ctx), HttpStatus.OK)
    }

    /**
     * Sets the.

     * @param name
     * *        the name
     * *
     * @param level
     * *        the level
     * *
     * @return the response entity
     */
    @RequestMapping(value = ["set/{name}/{level}"], produces = [(MediaType.APPLICATION_JSON_VALUE)], method = [(RequestMethod.GET)], headers = ["Accept=application/json"])
    @ResponseBody
    operator fun set(@PathVariable("name") name: String, @PathVariable("level") level: Level): ResponseEntity<LogResponse> {
        val ctx = LogManager.getContext(false) as LoggerContext
        synchronized(ctx) {
            val config = ctx.getConfiguration()
            val loggerConfig = config.getLoggerConfig(name)
            if (name.equals(loggerConfig.getName(), ignoreCase = true)) {
                loggerConfig.setLevel(level)
            } else {
                val newloggerConfig = LoggerConfig(name, level, true)
                config.addLogger(name, newloggerConfig)
            }
            ctx.updateLoggers()
        }
        return ResponseEntity(listLoggers(ctx), HttpStatus.OK)
    }

    /**
     * Unset.

     * @param name
     * *        the name
     * *
     * @return the response entity
     */
    @RequestMapping(value = ["unset/{name}/"], produces = [(MediaType.APPLICATION_JSON_VALUE)], method = [(RequestMethod.GET)], headers = ["Accept=application/json"])
    @ResponseBody
    fun unset(@PathVariable("name") name: String): ResponseEntity<LogResponse> {
        val ctx = LogManager.getContext(false) as LoggerContext
        synchronized(ctx) {
            val config = ctx.getConfiguration()
            config.removeLogger(name)
            ctx.updateLoggers()
        }
        return ResponseEntity(listLoggers(ctx), HttpStatus.OK)
    }

    /**
     * Reset.

     * @return the response entity
     */
    @RequestMapping(value = ["reset"], produces = [(MediaType.APPLICATION_JSON_VALUE)], method = [(RequestMethod.GET)], headers = ["Accept=application/json"])
    @ResponseBody
    fun reset(): ResponseEntity<LogResponse> {
        val ctx = LogManager.getContext(false) as LoggerContext
        synchronized(ctx) {
            ctx.reconfigure()
            initInMemoryAppender()
        }
        return ResponseEntity(listLoggers(ctx), HttpStatus.OK)
    }

    private fun listLoggers(ctx: LoggerContext): LogResponse {
        val logResponse = LogResponse()
        synchronized(ctx) {
            ctx.configuration.loggers.forEach({ name, configuration ->
                logResponse.specs.add(
                    LogSpecification(name,
                                    configuration.level.name(),
                                    configuration.appenderRefs.stream().map(AppenderRef::getRef).collect(Collectors.toList<String>()))
                    )
            })
        }
        return logResponse

    }

    /**
     * Captures the given logger at the given level so it can be displayed directly by this controller.

     * @param name the name
     * *
     * @param level the level
     * *
     * @param append the append
     * *
     * @return the response entity
     */
    @RequestMapping(value = ["capture/{name}/{level}"], produces = [(MediaType.APPLICATION_JSON_VALUE)], method = [(RequestMethod.GET)], headers = ["Accept=application/json"])
    @ResponseBody
    fun capture(
            @PathVariable("name") name: String,
            @PathVariable("level") level: Level,
            @RequestParam(value = "append", defaultValue = "false") append: Boolean): ResponseEntity<LogResponse> {
        val ctx = LogManager.getContext(false) as LoggerContext
        synchronized(ctx) {
            if (inMemoryAppenderImpl != null) {
                val config = ctx.getConfiguration()
                //
                if (name.equals(config.getLoggerConfig(name).getName(), ignoreCase = true)) {
                    config.removeLogger(name)
                }
                //
                val ref = AppenderRef.createAppenderRef("InMemoryAppenderImplAppenderRef", level, null)
                val refs = arrayOf(ref)
                val loggerConfig = LoggerConfig.createLogger(append, level, name, "true", refs, null, config, null)
                loggerConfig.addAppender(inMemoryAppenderImpl!!, null, null)
                config.addLogger(name, loggerConfig)
                ctx.updateLoggers()
            }
        }
        return ResponseEntity(listLoggers(ctx), HttpStatus.OK)
    }

    /**
     * Frees the given logger from the appender used to be displayed directly by this controller.

     * @param name
     * *        the name
     * *
     * @param level
     * *        the level
     * *
     * @return the response entity
     */
    @RequestMapping(value = ["free/{name}/"], produces = [(MediaType.APPLICATION_JSON_VALUE)], method = [(RequestMethod.GET)], headers = ["Accept=application/json"])
    @ResponseBody
    fun free(@PathVariable("name") name: String): ResponseEntity<LogResponse> {
        val ctx = LogManager.getContext(false) as LoggerContext
        synchronized(ctx) {
            val config = ctx.configuration
            val loggerConfig = config.getLoggerConfig(name)
            if (name.equals(loggerConfig.name, ignoreCase = true)) {
                config.removeLogger(name)
                val newloggerConfig = LoggerConfig(name, loggerConfig.level, true)
                config.addLogger(name, newloggerConfig)
            }
            ctx.updateLoggers()
        }
        return ResponseEntity(listLoggers(ctx), HttpStatus.OK)
    }

    /**
     * Reset.

     * @param response the response
     * *
     * @return the response entity
     */
    @RequestMapping(value = ["captured"], produces = [(MediaType.APPLICATION_JSON_VALUE)], method = [(RequestMethod.GET)], headers = ["Accept=application/json"])
    fun captured(response: HttpServletResponse) {
        response.contentType = "text/plain"
        response.characterEncoding = "UTF-8"
        val ctx = LogManager.getContext(false) as LoggerContext
        synchronized(ctx) {
            if (inMemoryAppenderImpl != null) {
                inMemoryAppenderImpl!!.registeredEvents.stream().forEach { line ->
                    try {
                        response.writer.write(line)
                    } catch (e: Exception) {
                        log.error("Error showing captured logs. Ironic, huh?", e)
                    }
                }
            }
        }
    }

}
