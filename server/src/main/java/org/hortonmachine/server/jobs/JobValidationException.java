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
package org.hortonmachine.server.jobs;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The values of a job are not valid.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class JobValidationException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    private final Map<String, String> errors;

    /**
     * @param errors the error messages by parameter name.
     */
    public JobValidationException( Map<String, String> errors ) {
        super(String.join(" ", errors.values()));
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    /**
     * @return the error messages by parameter name.
     */
    public Map<String, String> getErrors() {
        return errors;
    }
}
