package org.greeneyed.summer.config.enablers

import org.greeneyed.summer.config.Log4jMDCFilterConfiguration
import org.greeneyed.summer.config.SummerGlobalConfiguration
import org.greeneyed.summer.config.SummerWebConfig
import org.greeneyed.summer.config.XsltConfiguration
import org.greeneyed.summer.controller.HealthController
import org.greeneyed.summer.controller.Log4JController
import org.greeneyed.summer.util.ActuatorCustomizer
import org.springframework.context.annotation.ImportSelector
import org.springframework.core.annotation.AnnotationAttributes
import org.springframework.core.type.AnnotationMetadata

import java.util.ArrayList

class EnableSummerImportSelector : ImportSelector {

    override fun selectImports(importingClassMetadata: AnnotationMetadata): Array<String> {
        val attributes = AnnotationAttributes.fromMap(
                importingClassMetadata.getAnnotationAttributes(EnableSummer::class.java.name, false))
        val endPointsToEnable = ArrayList<String>()
        if (attributes.getBoolean("message_source")) {
            endPointsToEnable.add(SummerGlobalConfiguration::class.java.name)
        }
        if (attributes.getBoolean("log4j")) {
            endPointsToEnable.add(Log4JController::class.java.name)
        }
        if (attributes.getBoolean("log4j_filter")) {
            endPointsToEnable.add(Log4jMDCFilterConfiguration::class.java.name)
        }
        if (attributes.getBoolean("health")) {
            endPointsToEnable.add(HealthController::class.java.name)
        }
        if (attributes.getBoolean("actuator_customizer")) {
            endPointsToEnable.add(ActuatorCustomizer::class.java.name)
        }
        if (attributes.getBoolean("xslt_view")) {
            endPointsToEnable.add(XsltConfiguration::class.java.name)
        }
        if (attributes.getBoolean("xml_view_pooling")) {
            endPointsToEnable.add(SummerWebConfig::class.java.name)
        }
        return endPointsToEnable.toTypedArray()
    }
}