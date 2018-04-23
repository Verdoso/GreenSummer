package org.greeneyed.summer.config;

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


import java.util.Arrays;
import java.util.HashSet;

import org.greeneyed.summer.util.autoformatter.AutoregisterFormatterRegistrar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;

@Configuration
public class CustomConversionServiceConfiguration {
	
	@Autowired
	private AutoregisterFormatterRegistrar autoregisterFormatterRegistrar;

	@Bean
	public FormattingConversionServiceFactoryBean applicationConversionService() {
		final FormattingConversionServiceFactoryBean formattingConversionServiceFactoryBean = new FormattingConversionServiceFactoryBean();
		formattingConversionServiceFactoryBean.setFormatterRegistrars(new HashSet<>(Arrays.asList(autoregisterFormatterRegistrar)));
		return formattingConversionServiceFactoryBean;
	}

}
