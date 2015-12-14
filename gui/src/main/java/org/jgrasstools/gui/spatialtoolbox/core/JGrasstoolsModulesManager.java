/*
 * Stage - Spatial Toolbox And Geoscript Environment 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * (http://www.eclipse.org/legal/epl-v10.html).
 */
package org.jgrasstools.gui.spatialtoolbox.core;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.jgrasstools.Modules;
import org.jgrasstools.gears.JGrassGears;
import org.jgrasstools.gears.libs.logging.JGTLogger;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.hortonmachine.HortonMachine;
import org.jgrasstools.lesto.Lesto;

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
public class JGrasstoolsModulesManager {

    private static JGrasstoolsModulesManager modulesManager;

    private List<String> loadedJarsList = new ArrayList<String>();
    private List<String> modulesJarsList = new ArrayList<String>();

    private TreeMap<String, List<ModuleDescription>> modulesMap = new TreeMap<String, List<ModuleDescription>>();

    private URLClassLoader jarClassloader;

    private JGrasstoolsModulesManager() {
    }

    public synchronized static JGrasstoolsModulesManager getInstance() {
        if (modulesManager == null) {
            modulesManager = new JGrasstoolsModulesManager();
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
        LinkedHashMap<String, Class< ? >> moduleNames2Classes = Modules.getInstance().moduleName2Class;
        // LinkedHashMap<String, List<ClassField>> moduleName2Fields =
        // Modules.getInstance().moduleName2Fields;

        LinkedHashMap<String, Class< ? >> lestoModuleNames2Class = Lesto.getInstance().moduleName2Class;
        // LinkedHashMap<String, List<ClassField>> lestoModuleName2Fields =
        // Lesto.getInstance().moduleName2Fields;

        // also gather horton and gears
        HortonMachine.getInstance();
        JGrassGears.getInstance();

        for( Entry<String, Class< ? >> entry : lestoModuleNames2Class.entrySet() ) {
            String name = entry.getKey();
            if (name.startsWith("Oms")) {
                continue;
            }
            moduleNames2Classes.put(name, entry.getValue());
        }
        // moduleNames2Classes.putAll(lestoModuleNames2Class);
        // for( Entry<String, List<ClassField>> entry : lestoModuleName2Fields.entrySet() ) {
        // String name = entry.getKey();
        // if (name.startsWith("Oms")) {
        // continue;
        // }
        // moduleName2Fields.put(name, entry.getValue());
        // }
        // moduleName2Fields.putAll(lestoModuleName2Fields);

        Collection<Class< ? >> classesList = moduleNames2Classes.values();
        for( Class< ? > moduleClass : classesList ) {
            try {
                String simpleName = moduleClass.getSimpleName();

                UI uiHints = moduleClass.getAnnotation(UI.class);
                if (uiHints != null) {
                    String uiHintStr = uiHints.value();
                    if (uiHintStr.contains(JGTConstants.HIDE_UI_HINT)) {
                        continue;
                    }
                }

                Label category = moduleClass.getAnnotation(Label.class);
                String categoryStr = JGTConstants.OTHER;
                if (category != null && categoryStr.trim().length() > 1) {
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

                if (categoryStr.equals(JGTConstants.GRIDGEOMETRYREADER) || categoryStr.equals(JGTConstants.RASTERREADER)
                        || categoryStr.equals(JGTConstants.RASTERWRITER) || categoryStr.equals(JGTConstants.FEATUREREADER)
                        || categoryStr.equals(JGTConstants.FEATUREWRITER) || categoryStr.equals(JGTConstants.GENERICREADER)
                        || categoryStr.equals(JGTConstants.GENERICWRITER) || categoryStr.equals(JGTConstants.HASHMAP_READER)
                        || categoryStr.equals(JGTConstants.HASHMAP_WRITER) || categoryStr.equals(JGTConstants.LIST_READER)
                        || categoryStr.equals(JGTConstants.LIST_WRITER)) {
                    // ignore for now
                } else {
                    List<ModuleDescription> modulesList4Category = modulesMap.get(categoryStr);
                    if (modulesList4Category == null) {
                        modulesList4Category = new ArrayList<ModuleDescription>();
                        modulesMap.put(categoryStr, modulesList4Category);
                    }
                    modulesList4Category.add(module);
                }

            } catch (NoClassDefFoundError e) {
                if (moduleClass != null)
                    JGTLogger.logError(this, "ERROR", e.getCause());
            }
        }

        // sort
        Set<Entry<String, List<ModuleDescription>>> entrySet = modulesMap.entrySet();
        for( Entry<String, List<ModuleDescription>> entry : entrySet ) {
            Collections.sort(entry.getValue(), new ModuleDescription.ModuleDescriptionNameComparator());
        }
    }

    private void addInput( Access access, ModuleDescription module ) throws Exception {
        Field field = access.getField();
        Description descriptionAnn = field.getAnnotation(Description.class);
        String descriptionStr = "No description available";
        if (descriptionAnn != null) {
            descriptionStr = AnnotationUtilities.getLocalizedDescription(descriptionAnn);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(descriptionStr);

        Unit unitAnn = field.getAnnotation(Unit.class);
        if (unitAnn != null) {
            sb.append(" [");
            sb.append(unitAnn.value());
            sb.append("]");
        }
        Range rangeAnn = field.getAnnotation(Range.class);
        if (rangeAnn != null) {
            sb.append(" [");
            sb.append(rangeAnn.min());
            sb.append(" ,");
            sb.append(rangeAnn.max());
            sb.append("]");
        }
        descriptionStr = sb.toString();

        String fieldName = field.getName();
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

        module.addInput(fieldName, fieldClass.getCanonicalName(), descriptionStr, defaultValue, uiHint);
    }

    private void addOutput( Access access, ModuleDescription module ) throws Exception {
        Field field = access.getField();
        Description descriptionAnn = field.getAnnotation(Description.class);
        String descriptionStr = "No description available";
        if (descriptionAnn != null) {
            descriptionStr = AnnotationUtilities.getLocalizedDescription(descriptionAnn);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(descriptionStr);

        Unit unitAnn = field.getAnnotation(Unit.class);
        if (unitAnn != null) {
            sb.append(" [");
            sb.append(unitAnn.value());
            sb.append("]");
        }
        Range rangeAnn = field.getAnnotation(Range.class);
        if (rangeAnn != null) {
            sb.append(" [");
            sb.append(rangeAnn.min());
            sb.append(" ,");
            sb.append(rangeAnn.max());
            sb.append("]");
        }
        descriptionStr = sb.toString();

        String fieldName = field.getName();
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

        module.addOutput(fieldName, fieldClass.getCanonicalName(), descriptionStr, defaultValue, uiHint);
    }

}
