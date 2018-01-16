package org.greeneyed.summer.util

import org.greeneyed.summer.config.JoltViewConfiguration
import org.greeneyed.summer.config.JoltViewConfiguration.Companion.JSON_SOURCE_TAG
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.ModelAndView

class JoltModelAndView : ModelAndView {
    constructor(viewName: String, modelObject: Any, status: HttpStatus):
        super(SummerJoltView(viewName, ApplicationContextProvider.applicationContext?.getBean(JoltViewConfiguration::class.java))
                            , JSON_SOURCE_TAG
                            , modelObject){
        setStatus(status)
    }
}