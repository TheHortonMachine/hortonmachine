/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.server.web;

import java.util.LinkedHashMap;

/**
 * The values of a template, by the names used in it.
 *
 * <p>The values are texts, booleans for the <code>{{#name}}</code> sections, lists of models for the
 * repeated parts; a <code>null</code> or missing value is false, as an empty list.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Model extends LinkedHashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    /**
     * @param namesAndValues the names and the values, alternated.
     */
    public static Model of( Object... namesAndValues ) {
        Model model = new Model();
        for( int i = 0; i < namesAndValues.length; i += 2 ) {
            model.put((String) namesAndValues[i], namesAndValues[i + 1]);
        }
        return model;
    }

    /**
     * @return this model, with the value added.
     */
    public Model with( String name, Object value ) {
        put(name, value);
        return this;
    }
}
