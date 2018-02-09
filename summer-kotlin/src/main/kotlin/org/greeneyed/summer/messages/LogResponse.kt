package org.greeneyed.summer.messages;

import java.net.InetAddress


/**
 * The Class LogSpecification.
 */
public class LogResponse(
        val host: String = LOCAL_HOSTNAME,
        val specs: MutableList<LogSpecification> = mutableListOf<LogSpecification>()
) {
    companion object {
        val LOCAL_HOSTNAME: String by lazy { InetAddress.getLocalHost().hostName }
    }
}
