package org.greeneyed.summer.config.enablers;

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


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(EnableSummerImportSelector.class)
public @interface EnableSummer {

    boolean message_source() default true;

    boolean config_inspector() default true;

    boolean log4j() default true;

    boolean logback() default false;

    boolean slf4j_filter() default true;

    boolean health() default true;

    boolean actuator_customizer() default true;

    boolean xslt_view() default false;

    boolean xml_view_pooling() default false;

    boolean jolt_view() default false;

    boolean caffeine_cache() default false;

    boolean log_operations() default false;

    boolean fomatter_registrar() default true;

}
