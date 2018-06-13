package org.greeneyed.summer.util.autoformatter;

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


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.stereotype.Component;

// Based on an idea read at https://stackoverflow.com/questions/13778099/register-converters-and-converterfactories-with-annotations-in-spring-3
@Component
public class AutoregisterFormatterRegistrar implements FormatterRegistrar {

    /**
     * All {@link Converter} Beans with {@link AutoRegistered} annotation. If
     * spring does not find any matching bean, then the List is {@code null}!.
     */
    @Autowired(required = false)
    @AutoRegistered
    private List<Converter<?, ?>> autoRegisteredConverters;

    /**
     * All {@link Converter} Beans with {@link AutoRegistered} annotation. If
     * spring does not find any matching bean, then the List is {@code null}!.
     */
    @Autowired(required = false)
    @AutoRegistered
    private List<GenericConverter> autoRegisteredGenericConverters;

    @Override
    public void registerFormatters(final FormatterRegistry registry) {
        if (autoRegisteredConverters != null) {
            for (Converter<?, ?> converter : autoRegisteredConverters) {
                registry.addConverter(converter);
            }
        }
        if (autoRegisteredGenericConverters != null) {
            for (GenericConverter converter : autoRegisteredGenericConverters) {
                registry.addConverter(converter);
            }
        }
    }
}
