package org.greeneyed.summer.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * The Class HealthController.
 */
@Controller
@RequestMapping( "/health" )
class HealthController {

    enum class STATUS {
        OK, DISABLED
    }

    private var status = STATUS.OK

    /**
     * @return The status
     */
    @RequestMapping(method = [(RequestMethod.GET)], produces = [(MediaType.APPLICATION_JSON_VALUE)])
    @ResponseBody
    fun checkHealth() = currentStatus()

    /**
     * Enable.

     * @return the response entity
     */
    @RequestMapping(value = ["/enable"], method = [(RequestMethod.GET)], produces = [(MediaType.APPLICATION_JSON_VALUE)])
    @ResponseBody
    fun enable(): ResponseEntity<Map<String, String>> {
        status = STATUS.OK
        return currentStatus()
    }

    /**
     * Disable.

     * @return the response entity
     */
    @RequestMapping(value = ["/disable"], method = [(RequestMethod.GET)], produces = [(MediaType.APPLICATION_JSON_VALUE)])
    @ResponseBody
    fun disable(): ResponseEntity<Map<String, String>> {
        status = STATUS.DISABLED
        return currentStatus()
    }

    private fun currentStatus() = ResponseEntity(mapOf<String,String>("status" to status.name), HttpStatus.OK)
}
