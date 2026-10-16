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
package org.hortonmachine.cli;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMParameterKind;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

import oms3.annotations.Execute;

/**
 * Runs a module in this process, with the values of its parameters given as text.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ModuleRunner {
    private final ModuleDescriptor module;

    public ModuleRunner( ModuleDescriptor module ) {
        this.module = module;
    }

    /**
     * Run the module.
     *
     * @param values the values by parameter name, checked with {@link ModuleDescriptor#check(Map)}.
     * @param pm the progress monitor.
     * @return the outputs by parameter name, in the order of the parameters: the values computed
     *         by the module that are not <code>null</code> and the paths of the outputs set in the values.
     * @throws IllegalArgumentException if a value is not valid.
     * @throws Exception the error of the module.
     */
    public Map<String, Object> run( Map<String, String> values, IHMProgressMonitor pm ) throws Exception {
        Map<String, String> errors = module.check(values);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors.values()));
        }

        Class< ? extends HMModel> moduleClass = module.getModuleClass();
        HMModel instance = moduleClass.getDeclaredConstructor().newInstance();
        for( Map.Entry<String, String> entry : values.entrySet() ) {
            Parameter parameter = module.getParameter(entry.getKey());
            parameter.field.set(instance, parameter.convert(entry.getValue()));
        }
        instance.pm = pm;

        Method executeMethod = null;
        for( Method method : moduleClass.getMethods() ) {
            if (method.isAnnotationPresent(Execute.class) && method.getParameterCount() == 0) {
                executeMethod = method;
                break;
            }
        }
        if (executeMethod == null) {
            throw new IllegalStateException("The module " + module.getName() + " has no method to execute.");
        }

        try {
            executeMethod.invoke(instance);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw e;
        }

        Map<String, Object> outputs = new LinkedHashMap<>();
        for( Parameter parameter : module.getParameters() ) {
            if (parameter.isComputed) {
                Object value = parameter.field.get(instance);
                if (value != null) {
                    outputs.put(parameter.name, value);
                }
            } else if (parameter.kind == HMParameterKind.OUTPUT && values.containsKey(parameter.name)) {
                outputs.put(parameter.name, values.get(parameter.name));
            }
        }
        return outputs;
    }

    /**
     * Convert a value to text, arrays as [a, b, c].
     */
    public static String valueToString( Object value ) {
        if (value.getClass().isArray()) {
            StringBuilder sb = new StringBuilder("[");
            for( int i = 0; i < Array.getLength(value); i++ ) {
                if (i > 0) {
                    sb.append(", ");
                }
                Object element = Array.get(value, i);
                sb.append(element != null && element.getClass().isArray() ? valueToString(element) : element);
            }
            return sb.append("]").toString();
        }
        return value.toString();
    }
}
