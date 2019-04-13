package org.greeneyed.summer.util;

/*
 * #%L
 * Summer
 * %%
 * Copyright (C) 2018 GreenEyed (Daniel Lopez)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.greeneyed.summer.config.JoltViewConfiguration;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SummerJoltView extends MappingJackson2JsonView {
    private final String joltSpecName;
    private final JoltViewConfiguration joltViewConfiguration;
    private boolean transform = true;
    private boolean refresh = false;
    
    public SummerJoltView(final String joltSpecName, final JoltViewConfiguration joltViewConfiguration) {
        this.joltSpecName = joltSpecName;
        this.joltViewConfiguration = joltViewConfiguration;
        this.setModelKey(JoltViewConfiguration.JSON_SOURCE_TAG);
        this.setExtractValueFromSingleKeyModel(true);
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        final boolean devMode = joltViewConfiguration.isDevMode();
        final String showSourceFlag = request.getParameter(JoltViewConfiguration.SHOW_JSON_SOURCE_FLAG);
        final String refreshFlag = request.getParameter(JoltViewConfiguration.REFRESH_SPEC_FLAG);
        transform = !(devMode && Boolean.parseBoolean(showSourceFlag));
        refresh = devMode && (refreshFlag==null || Boolean.parseBoolean(refreshFlag));
        super.render(model, request, response);
    }

    /**
     * Write the actual JSON content to the stream.
     * 
     * @param stream
     *        the output stream to use
     * @param object
     *        the value to be rendered, as returned from {@link #filterModel}
     * @throws IOException
     *         if writing failed
     */
    @Override
    protected void writeContent(OutputStream stream, Object object) throws IOException {
        if (transform) {
            Chainr chainr = joltViewConfiguration.getChainr(joltSpecName, refresh);
            try (ByteArrayOutputStream theBAOS = new ByteArrayOutputStream()) {
                super.writeContent(theBAOS, object);
                theBAOS.flush();
                super.writeContent(stream, chainr.transform(JsonUtils.jsonToObject(theBAOS.toString(this.getEncoding().getJavaName()))));
            }
        } else {
            super.writeContent(stream, object);
        }
    }
}
