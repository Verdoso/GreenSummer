package org.greeneyed.summer.util

import org.springframework.web.servlet.view.json.MappingJackson2JsonView
import com.bazaarvoice.jolt.modifier.function.Objects.toString
import com.sun.deploy.trace.Trace.flush
import java.io.ByteArrayOutputStream
import com.bazaarvoice.jolt.JsonUtils.classpathToList
import com.bazaarvoice.jolt.Chainr
import com.bazaarvoice.jolt.JsonUtils
import org.greeneyed.summer.config.JoltViewConfiguration
import java.io.IOException
import java.io.OutputStream
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest



class SummerJoltView(private val joltSpecName: String, private val joltViewConfiguration: JoltViewConfiguration?) : MappingJackson2JsonView() {

    private var transform = true

    init {
        setModelKey(JoltViewConfiguration.JSON_SOURCE_TAG)
        setExtractValueFromSingleKeyModel(true)
    }

    @Throws(Exception::class)
    override fun render(model: Map<String, *>, request: HttpServletRequest, response: HttpServletResponse) {
        transform = !(joltViewConfiguration?.devMode==true && java.lang.Boolean.parseBoolean(request.getParameter(JoltViewConfiguration.SHOW_JSON_SOURCE_FLAG)))
        super.render(model, request, response)
    }

    /**
     * Write the actual JSON content to the stream.
     *
     * @param stream
     * the output stream to use
     * @param object
     * the value to be rendered, as returned from [.filterModel]
     * @throws IOException
     * if writing failed
     */
    @Throws(IOException::class)
    override fun writeContent(stream: OutputStream, content: Any) {
        val chainr = Chainr.fromSpec(JsonUtils.classpathToList(joltViewConfiguration?.specPrefix + joltSpecName + joltViewConfiguration?.specSuffix))
        if (transform) {
            ByteArrayOutputStream().use { theBAOS ->
                super.writeContent(theBAOS, content)
                theBAOS.flush()
                super.writeContent(stream, chainr.transform(JsonUtils.jsonToObject(theBAOS.toString(this.encoding.javaName))))
            }
        } else {
            super.writeContent(stream, content)
        }

    }

}