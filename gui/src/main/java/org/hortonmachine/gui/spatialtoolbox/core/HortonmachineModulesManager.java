/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gui.spatialtoolbox.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMModelRegistry;

import oms3.Access;
import oms3.ComponentAccess;
import oms3.annotations.Description;
import oms3.annotations.Label;
import oms3.annotations.Range;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

/**
 * Singleton in which the modules discovery and load/unload occurrs.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class HortonmachineModulesManager {
    private static HortonmachineModulesManager modulesManager;

    private TreeMap<String, List<ModuleDescription>> modulesMap = new TreeMap<String, List<ModuleDescription>>();

    private HortonmachineModulesManager() {
    }

    public synchronized static HortonmachineModulesManager getInstance() {
        if (modulesManager == null) {
            modulesManager = new HortonmachineModulesManager();
        }
        return modulesManager;
    }

    public TreeMap<String, List<ModuleDescription>> getModulesMap() {
        return modulesMap;
    }

    public void init() throws Exception {
        synchronized (modulesMap) {
            if (modulesMap.size() > 0) {
                return;
            }
        }
        // all modules registered via SPI, also those of external jars
        Map<String, Class< ? >> moduleNames2Classes = new LinkedHashMap<>();
        for( Class< ? extends HMModel> modelClass : HMModelRegistry.getModelClasses() ) {
            // the same modules of the command line and the QGIS plugin
            if (!moduleNames2Classes.containsKey(modelClass.getSimpleName()) && ModuleDescriptor.isAvailable(modelClass)) {
                moduleNames2Classes.putIfAbsent(modelClass.getSimpleName(), modelClass);
            }
        }

        Collection<Class< ? >> classesList = moduleNames2Classes.values();
        for( Class< ? > moduleClass : classesList ) {
            try {
                String simpleName = moduleClass.getSimpleName();

                Label category = moduleClass.getAnnotation(Label.class);
                String categoryStr = HMConstants.OTHER;
                if (category != null) {
                    categoryStr = category.value();
                }

                Description description = moduleClass.getAnnotation(Description.class);
                String descrStr = null;
                if (description != null) {
                    descrStr = description.value();
                }
                Status status = moduleClass.getAnnotation(Status.class);

                ModuleDescription module = new ModuleDescription(moduleClass, categoryStr, descrStr, status);

                Object newInstance = null;
                try {
                    newInstance = moduleClass.newInstance();
                } catch (Throwable e) {
                    // ignore module
                    continue;
                }
                try {
                    // generate the html docs
                    String className = module.getClassName();
                    // FIXME
                    // SpatialToolboxUtils.generateModuleDocumentation(className);
                } catch (Exception e) {
                    // ignore doc if it breaks
                }

                ComponentAccess cA = new ComponentAccess(newInstance);

                Collection<Access> inputs = cA.inputs();
                for( Access access : inputs ) {
                    addInput(access, module);
                }

                Collection<Access> outputs = cA.outputs();
                for( Access access : outputs ) {
                    addOutput(access, module);
                }

                List<ModuleDescription> modulesList4Category = modulesMap.get(categoryStr);
                if (modulesList4Category == null) {
                    modulesList4Category = new ArrayList<ModuleDescription>();
                    modulesMap.put(categoryStr, modulesList4Category);
                }
                modulesList4Category.add(module);

            } catch (Exception | NoClassDefFoundError e) {
                if (moduleClass != null)
                    Logger.INSTANCE.insertError("", "ERROR in module " + moduleClass.getName(), e);
            }
        }

        // sort
        Set<Entry<String, List<ModuleDescription>>> entrySet = modulesMap.entrySet();
        for( Entry<String, List<ModuleDescription>> entry : entrySet ) {
            Collections.sort(entry.getValue(), new ModuleDescription.ModuleDescriptionNameComparator());
        }
    }

    private void addInput( Access access, ModuleDescription module ) throws Exception {
        addField(access, module, true);
    }

    private void addOutput( Access access, ModuleDescription module ) throws Exception {
        addField(access, module, false);
    }

    private void addField( Access access, ModuleDescription module, boolean isInput ) throws Exception {
        Field field = access.getField();
        String fieldName = field.getName();
        if (doIgnore(fieldName)) {
            return;
        }
        Description descriptionAnn = field.getAnnotation(Description.class);
        String descriptionStr = "No description available";
        if (descriptionAnn != null) {
            descriptionStr = AnnotationUtilities.getLocalizedDescription(descriptionAnn);
        }

        Class< ? > fieldClass = field.getType();
        Object fieldValue = access.getFieldValue();
        String defaultValue = ""; //$NON-NLS-1$
        if (fieldValue != null) {
            defaultValue = fieldValue.toString();
        }

        UI uiHintAnn = field.getAnnotation(UI.class);
        String uiHint = null;
        if (uiHintAnn != null) {
            uiHint = uiHintAnn.value();
        }

        FieldData fieldData;
        if (isInput) {
            fieldData = module.addInput(fieldName, fieldClass.getCanonicalName(), descriptionStr, defaultValue, uiHint);
        } else {
            fieldData = module.addOutput(fieldName, fieldClass.getCanonicalName(), descriptionStr, defaultValue, uiHint);
        }
        Unit unitAnn = field.getAnnotation(Unit.class);
        if (unitAnn != null && !unitAnn.value().isBlank()) {
            fieldData.unit = unitAnn.value().trim();
        }
        Range rangeAnn = field.getAnnotation(Range.class);
        if (rangeAnn != null) {
            fieldData.range = formatRange(rangeAnn);
        }
    }

    /**
     * Format a range, leaving out the bounds that are the annotation defaults.
     */
    private static String formatRange( Range range ) {
        boolean hasMin = range.min() != Double.MIN_VALUE;
        boolean hasMax = range.max() != Double.MAX_VALUE;
        if (hasMin && hasMax) {
            return "[" + formatNumber(range.min()) + ", " + formatNumber(range.max()) + "]";
        } else if (hasMin) {
            return "\u2265 " + formatNumber(range.min());
        } else if (hasMax) {
            return "\u2264 " + formatNumber(range.max());
        }
        return null;
    }

    private static String formatNumber( double value ) {
        if (value == Math.rint(value) && Math.abs(value) < 1E15) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private boolean doIgnore( String fieldName ) {
        return fieldName.equals("doProcess");
    }

}
