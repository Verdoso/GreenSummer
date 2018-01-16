package org.greeneyed.summer.config.enablers

import org.springframework.context.annotation.Import
import kotlin.annotation.Retention

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Import(EnableSummerImportSelector::class)
annotation class EnableSummer(
            val message_source: Boolean = true
            , val log4j: Boolean = true
            , val log4j_filter: Boolean = true
            , val health: Boolean = true
            , val actuator_customizer: Boolean = true
            , val xslt_view: Boolean = false
            , val xml_view_pooling: Boolean = false)
