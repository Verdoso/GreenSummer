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


import java.util.Collection;
import java.util.StringJoiner;

/**
 * The Class ObjectJoiner.
 */
public class ObjectJoiner {

    private ObjectJoiner() {
    }

    /**
     * Join.
     *
     * @param separator the string to use as separator
     * @param arguments the array of objects (varargs) to join
     * @return the string resulting from joining the arguments (.toString()) together separated by the separator
     */
    public static String join(CharSequence separator, Object... arguments) {
        StringJoiner st = new StringJoiner(separator);
        if (arguments != null) {
            for (Object object : arguments) {
                if (object != null) {
                    if (object instanceof String) {
                        st.add((String) object);
                    } else {
                        st.add(object.toString());
                    }
                }
            }
        }
        return st.toString();
    }

    /**
     * Join.
     *
     * @param separator the string to use as separator
     * @param arguments the collection of objects to join
     * @return the string resulting from joining the arguments (.toString()) together separated by the separator
     */
    public static String join(CharSequence separator, Collection<? extends Object> arguments) {
        StringJoiner st = new StringJoiner(separator);
        if (arguments != null) {
            for (Object object : arguments) {
                if (object != null) {
                    if (object instanceof String) {
                        st.add((String) object);
                    } else {
                        st.add(object.toString());
                    }
                }
            }
        }
        return st.toString();
    }

    /**
     * Simply join the arguments, without separator.
     *
     * @param arguments The objects to join
     * @return the string resulting from joining the arguments (.toString()) together
     */
    public static String simplyJoin(Object... arguments) {
        return join("", arguments);
    }
}
