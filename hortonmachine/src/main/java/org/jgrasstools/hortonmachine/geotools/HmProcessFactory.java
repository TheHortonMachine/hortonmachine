/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.hortonmachine.geotools;

import java.awt.RenderingHints.Key;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.jgrasstools.gears.JGrassGears;
import org.jgrasstools.gears.libs.modules.ClassField;
import org.jgrasstools.hortonmachine.HortonMachine;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

/**
 * ProcessFactory for the horton machine modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HmProcessFactory implements ProcessFactory {

    private static final String VERSION_STRING = "0.1-SNAPSHOT";
    private static final String namespace = "org.jgrasstools.hortonmachine";
    private LinkedHashMap<String, Class< ? >> modulename2class;

    public Process create( Name name ) {
        String moduleName = name.getLocalPart();
        Class< ? > moduleClass = modulename2class.get(moduleName);
        try {
            Object processObj = moduleClass.newInstance();
            if (processObj instanceof Process) {
                Process process = (Process) processObj;
                return process;
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public InternationalString getDescription( Name name ) {
        return null;
    }

    public Set<Name> getNames() {
        Set<Name> names = new LinkedHashSet<Name>();
        modulename2class = HortonMachine.getInstance().moduleName2Class;
        Set<String> modulesNames = modulename2class.keySet();
        for( String name : modulesNames ) {
            names.add(new NameImpl(namespace, name));
        }
        return names;
    }

    public Map<String, Parameter< ? >> getParameterInfo( Name name ) {
        String moduleName = name.getLocalPart();
        LinkedHashMap<String, List<ClassField>> modulename2fields = HortonMachine.getInstance().moduleName2Fields;
        List<ClassField> list = modulename2fields.get(moduleName);

        Map<String, Parameter< ? >> input = new LinkedHashMap<String, Parameter< ? >>();
        for( ClassField classField : list ) {
            if (classField.isIn) {
                String fieldName = classField.fieldName;
                Parameter< ? > param = new Parameter(fieldName, classField.fieldClass, fieldName, fieldName);
                input.put(param.key, param);
            }
        }
        return input;
    }

    public Map<String, Parameter< ? >> getResultInfo( Name name, Map<String, Object> parameters ) throws IllegalArgumentException {
        String moduleName = name.getLocalPart();
        LinkedHashMap<String, List<ClassField>> modulename2fields = JGrassGears.getInstance().moduleName2Fields;
        List<ClassField> list = modulename2fields.get(moduleName);

        Map<String, Parameter< ? >> output = new LinkedHashMap<String, Parameter< ? >>();
        for( ClassField classField : list ) {
            if (classField.isOut) {
                String fieldName = classField.fieldName;
                String fieldDescription = classField.fieldDescription;
                Parameter< ? > param = new Parameter(fieldName, classField.fieldClass, fieldName, fieldDescription);
                output.put(param.key, param);
            }
        }
        return output;
    }

    public InternationalString getTitle() {
        return null;
    }

    public InternationalString getTitle( Name name ) {
        return null;
    }

    public String getVersion( Name name ) {
        return VERSION_STRING;
    }

    public boolean supportsProgress( Name name ) {
        return true;
    }

    public boolean isAvailable() {
        return true;
    }

    public Map<Key, ? > getImplementationHints() {
        return null;
    }

}
