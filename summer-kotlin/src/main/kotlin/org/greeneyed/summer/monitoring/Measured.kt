package org.greeneyed.summer.monitoring

import kotlin.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class Measured(val value: String = "")
