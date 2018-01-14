package org.greeneyed.summer.monitoring;

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


import org.slf4j.MDC;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfiledMeasure {

    public static final String MDC_UUID_TOKEN_KEY = "Log4UUIDFilter.UUID";

    private final String name;
    private final long value;
    private final boolean showValue;
    private final String[] tags;
    @Builder.Default
    private final String token = MDC.get(MDC_UUID_TOKEN_KEY);

}
