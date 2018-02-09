package org.greeneyed.summer.util

import org.springframework.context.ApplicationContextAware
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext



class ApplicationContextProvider : ApplicationContextAware {

    companion object {
        var applicationContext: ApplicationContext? = null
    }

    @Throws(BeansException::class)
    override fun setApplicationContext(ac: ApplicationContext) {
        ApplicationContextProvider.applicationContext = ac
    }
}