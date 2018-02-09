package org.greeneyed.summer.messages;

/**
 * The Class LogSpecification.
 */
data class LogSpecification(
        val name : String?,
        val level: String ?,
        val appenders: MutableList<String>?
)
