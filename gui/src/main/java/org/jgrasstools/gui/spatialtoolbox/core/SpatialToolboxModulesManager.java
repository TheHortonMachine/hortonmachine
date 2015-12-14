///*
// * Stage - Spatial Toolbox And Geoscript Environment 
// * (C) HydroloGIS - www.hydrologis.com 
// *
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the Eclipse Public License v1.0
// * (http://www.eclipse.org/legal/epl-v10.html).
// */
//package org.jgrasstools.gui.spatialtoolbox.core;
//
//import java.io.File;
//import java.io.FilenameFilter;
//import java.io.IOException;
//import java.io.InputStream;
//import java.lang.reflect.Field;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Map.Entry;
//import java.util.Set;
//import java.util.TreeMap;
//
//import org.gvsig.andami.PluginServices;
//import org.gvsig.andami.PluginsLocator;
//import org.gvsig.andami.PluginsManager;
//import org.jgrasstools.gears.libs.logging.JGTLogger;
//import org.jgrasstools.gears.libs.modules.JGTConstants;
//import org.jgrasstools.gvsig.base.JGrasstoolsExtension;
//import org.jgrasstools.gvsig.spatialtoolbox.core.ModuleDescription;
//import org.jgrasstools.gvsig.spatialtoolbox.core.utils.AnnotationUtilities;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import oms3.Access;
//import oms3.ComponentAccess;
//import oms3.annotations.Description;
//import oms3.annotations.Label;
//import oms3.annotations.Range;
//import oms3.annotations.Status;
//import oms3.annotations.UI;
//import oms3.annotations.Unit;
//import oms3.util.Components;
//
///**
// * Singleton in which the modules discovery and load/unload occurrs.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//@SuppressWarnings("nls")
//public class SpatialToolboxModulesManager {
//
//    private static SpatialToolboxModulesManager modulesManager;
//
//    private List<String> loadedJarsList = new ArrayList<String>();
//    private List<String> modulesJarsList = new ArrayList<String>();
//    private TreeMap<String, List<ModuleDescription>> modulesMap;
//
//    private URLClassLoader jarClassloader;
//
//    private SpatialToolboxModulesManager() {
//        getModulesJars(false);
//    }
//
//
//    public List<String> getModulesJars( boolean onlyModules ) {
//        List<String> jarsPathList = new ArrayList<String>();
//
//        // FIXME path!
//        PluginsManager manager = PluginsLocator.getManager();
//        // applicat manager.getApplicationFolder() ex
//        // /home/hydrologis/SOFTWARE/GVSIG/gvsig-desktop-2.2.0-2313-final-lin-x86_64/
//        PluginServices plugin = manager.getPlugin(JGrasstoolsExtension.class);
//        File pluginDirectory = plugin.getPluginDirectory();
//        // plugin.getPluginHomeFolder()
//        // String libsPath =
//        // "/home/hydrologis/SOFTWARE/GVSIG/gvsig-desktop-2.2.0-2313-final-lin-x86_64/gvSIG/extensiones/org.jgrasstools.gvsig.base/lib/";
//   
//        File libsFolder = new File(pluginDirectory, "lib");
//
//        /*
//         * load modules
//         */
//        if (libsFolder != null) {
//            modulesJarsList.clear();
//            JGTLogger.logInfo(this, "Searching module libraries in: " + libsFolder.getAbsolutePath());
//            File[] extraJars = libsFolder.listFiles(new FilenameFilter(){
//                public boolean accept( File dir, String name ) {
//                    return name.endsWith(".jar") && name.startsWith("jgt-");
//                }
//            });
//            for( File extraJar : extraJars ) {
//                addJar(extraJar.getAbsolutePath());
//                jarsPathList.add(extraJar.getAbsolutePath());
//                modulesJarsList.add(extraJar.getAbsolutePath());
//            }
//        }
//        /*
//         * load libs
//         */
//        if (!onlyModules) {
//            if (libsFolder != null) {
//                JGTLogger.logInfo(this,"Searching libs in: " + libsFolder.getAbsolutePath());
//                File[] extraJars = libsFolder.listFiles(new FilenameFilter(){
//                    public boolean accept( File dir, String name ) {
//                        return name.endsWith(".jar") && !name.startsWith("jgt-");
//                    }
//                });
//                for( File extraJar : extraJars ) {
//                    addJar(extraJar.getAbsolutePath());
//                    jarsPathList.add(extraJar.getAbsolutePath());
//                }
//            }
//        }
//
//        return jarsPathList;
//    }
//
//    public synchronized static SpatialToolboxModulesManager getInstance() {
//        if (modulesManager == null) {
//            modulesManager = new SpatialToolboxModulesManager();
//        }
//        return modulesManager;
//    }
//
//    /**
//     * Add a jar to the jars list.
//     * 
//     * @param newJar
//     *            the path to the new jar to add.
//     */
//    public void addJar( String newJar ) {
//        if (!loadedJarsList.contains(newJar)) {
//            loadedJarsList.add(newJar);
//        }
//    }
//
//    /**
//     * Remove a jar from the jars list.
//     * 
//     * @param removeJar
//     *            the jar to remove.
//     */
//    public void removeJar( String removeJar ) {
//        if (loadedJarsList.contains(removeJar)) {
//            loadedJarsList.remove(removeJar);
//        }
//    }
//
//    /**
//     * Remove all jars from the cache list.
//     */
//    public void clearJars() {
//        loadedJarsList.clear();
//    }
//
//    /**
//     * Browses the loaded jars searching for executable modules.
//     * 
//     * @param rescan
//     *            whther to scan the jars again. Isn't considered the first
//     *            time.
//     * @return the list of modules that are executable.
//     * @throws IOException
//     */
//    public TreeMap<String, List<ModuleDescription>> browseModules( boolean rescan ) {
//        try {
//            if (modulesMap == null) {
//                modulesMap = new TreeMap<String, List<ModuleDescription>>();
//            } else {
//                if (rescan) {
//                    modulesMap.clear();
//                } else {
//                    if (modulesMap.size() > 0) {
//                        return modulesMap;
//                    }
//                }
//            }
//
//            scanForModules();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            modulesMap = new TreeMap<String, List<ModuleDescription>>();
//        }
//
//        Set<Entry<String, List<ModuleDescription>>> entrySet = modulesMap.entrySet();
//        for( Entry<String, List<ModuleDescription>> entry : entrySet ) {
//            Collections.sort(entry.getValue(), new ModuleDescription.ModuleDescriptionNameComparator());
//        }
//
//        return modulesMap;
//    }
//
//    public List<ModuleDescription> cloneList( List<ModuleDescription> list ) {
//        List<ModuleDescription> copy = new ArrayList<ModuleDescription>();
//        for( ModuleDescription moduleDescription : list ) {
//            copy.add(moduleDescription.makeCopy());
//        }
//        return copy;
//    }
//
//    private void scanForModules() throws Exception {
//
//        if (modulesJarsList.size() == 0) {
//            return;
//        }
//
//        List<URL> urlList = new ArrayList<URL>();
//        JGTLogger.logInfo(this,"ADDED TO URL CLASSLOADER:");
//        for( int i = 0; i < modulesJarsList.size(); i++ ) {
//            String jarPath = modulesJarsList.get(i);
//            File jarFile = new File(jarPath);
//            if (!jarFile.exists()) {
//                continue;
//            }
//            urlList.add(jarFile.toURI().toURL());
//            JGTLogger.logInfo(this,"--> " + jarPath);
//        }
//        URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
//        List<Class< ? >> classesList = new ArrayList<Class< ? >>();
//
//        jarClassloader = new URLClassLoader(urls, this.getClass().getClassLoader());
//
//        JGTLogger.logInfo(this,"LOAD MODULES:");
//        // try the old and slow way
//        try {
//            JGTLogger.logInfo(this,"URLS TO LOAD:");
//            for( URL url : urls ) {
//                JGTLogger.logInfo(this,url.toExternalForm());
//            }
//            List<Class< ? >> allComponents = new ArrayList<Class< ? >>();
//            allComponents = Components.getComponentClasses(jarClassloader, urls);
//            classesList.addAll(allComponents);
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//
//        // clean up html docs in config area, it will be redone
//        // SpatialToolboxUtils.cleanModuleDocumentation(); FIXME
//
//        for( Class< ? > moduleClass : classesList ) {
//            try {
//                String simpleName = moduleClass.getSimpleName();
//
//                UI uiHints = moduleClass.getAnnotation(UI.class);
//                if (uiHints != null) {
//                    String uiHintStr = uiHints.value();
//                    if (uiHintStr.contains(JGTConstants.HIDE_UI_HINT)) {
//                        continue;
//                    }
//                }
//
//                Label category = moduleClass.getAnnotation(Label.class);
//                String categoryStr = JGTConstants.OTHER;
//                if (category != null && categoryStr.trim().length() > 1) {
//                    categoryStr = category.value();
//                }
//
//                Description description = moduleClass.getAnnotation(Description.class);
//                String descrStr = null;
//                if (description != null) {
//                    descrStr = description.value();
//                }
//                Status status = moduleClass.getAnnotation(Status.class);
//
//                ModuleDescription module = new ModuleDescription(moduleClass, categoryStr, descrStr, status);
//
//                Object newInstance = null;
//                try {
//                    newInstance = moduleClass.newInstance();
//                } catch (Throwable e) {
//                    // ignore module
//                    continue;
//                }
//                try {
//                    // generate the html docs
//                    String className = module.getClassName();
//                    // FIXME
//                    // SpatialToolboxUtils.generateModuleDocumentation(className);
//                } catch (Exception e) {
//                    // ignore doc if it breaks
//                }
//
//                ComponentAccess cA = new ComponentAccess(newInstance);
//
//                Collection<Access> inputs = cA.inputs();
//                for( Access access : inputs ) {
//                    addInput(access, module);
//                }
//
//                Collection<Access> outputs = cA.outputs();
//                for( Access access : outputs ) {
//                    addOutput(access, module);
//                }
//
//                if (categoryStr.equals(JGTConstants.GRIDGEOMETRYREADER)
//                        || categoryStr.equals(JGTConstants.RASTERREADER)
//                        || categoryStr.equals(JGTConstants.RASTERWRITER)
//                        || categoryStr.equals(JGTConstants.FEATUREREADER)
//                        || categoryStr.equals(JGTConstants.FEATUREWRITER)
//                        || categoryStr.equals(JGTConstants.GENERICREADER)
//                        || categoryStr.equals(JGTConstants.GENERICWRITER)
//                        || categoryStr.equals(JGTConstants.HASHMAP_READER)
//                        || categoryStr.equals(JGTConstants.HASHMAP_WRITER)
//                        || categoryStr.equals(JGTConstants.LIST_READER)
//                        || categoryStr.equals(JGTConstants.LIST_WRITER)) {
//                    // ignore for now
//                } else {
//                    List<ModuleDescription> modulesList4Category = modulesMap.get(categoryStr);
//                    if (modulesList4Category == null) {
//                        modulesList4Category = new ArrayList<ModuleDescription>();
//                        modulesMap.put(categoryStr, modulesList4Category);
//                    }
//                    modulesList4Category.add(module);
//                }
//
//            } catch (NoClassDefFoundError e) {
//                if (moduleClass != null)
//                    JGTLogger.logError(this, "ERROR", e.getCause());
//            }
//        }
//    }
//
//    private void addInput( Access access, ModuleDescription module ) throws Exception {
//        Field field = access.getField();
//        Description descriptionAnn = field.getAnnotation(Description.class);
//        String descriptionStr = "No description available";
//        if (descriptionAnn != null) {
//            descriptionStr = AnnotationUtilities.getLocalizedDescription(descriptionAnn);
//        }
//
//        StringBuilder sb = new StringBuilder();
//        sb.append(descriptionStr);
//
//        Unit unitAnn = field.getAnnotation(Unit.class);
//        if (unitAnn != null) {
//            sb.append(" [");
//            sb.append(unitAnn.value());
//            sb.append("]");
//        }
//        Range rangeAnn = field.getAnnotation(Range.class);
//        if (rangeAnn != null) {
//            sb.append(" [");
//            sb.append(rangeAnn.min());
//            sb.append(" ,");
//            sb.append(rangeAnn.max());
//            sb.append("]");
//        }
//        descriptionStr = sb.toString();
//
//        String fieldName = field.getName();
//        Class< ? > fieldClass = field.getType();
//        Object fieldValue = access.getFieldValue();
//
//        String defaultValue = ""; //$NON-NLS-1$
//        if (fieldValue != null) {
//            defaultValue = fieldValue.toString();
//        }
//
//        UI uiHintAnn = field.getAnnotation(UI.class);
//        String uiHint = null;
//        if (uiHintAnn != null) {
//            uiHint = uiHintAnn.value();
//        }
//
//        module.addInput(fieldName, fieldClass.getCanonicalName(), descriptionStr, defaultValue, uiHint);
//    }
//
//    private void addOutput( Access access, ModuleDescription module ) throws Exception {
//        Field field = access.getField();
//        Description descriptionAnn = field.getAnnotation(Description.class);
//        String descriptionStr = "No description available";
//        if (descriptionAnn != null) {
//            descriptionStr = AnnotationUtilities.getLocalizedDescription(descriptionAnn);
//        }
//        StringBuilder sb = new StringBuilder();
//        sb.append(descriptionStr);
//
//        Unit unitAnn = field.getAnnotation(Unit.class);
//        if (unitAnn != null) {
//            sb.append(" [");
//            sb.append(unitAnn.value());
//            sb.append("]");
//        }
//        Range rangeAnn = field.getAnnotation(Range.class);
//        if (rangeAnn != null) {
//            sb.append(" [");
//            sb.append(rangeAnn.min());
//            sb.append(" ,");
//            sb.append(rangeAnn.max());
//            sb.append("]");
//        }
//        descriptionStr = sb.toString();
//
//        String fieldName = field.getName();
//        Class< ? > fieldClass = field.getType();
//        Object fieldValue = access.getFieldValue();
//
//        String defaultValue = ""; //$NON-NLS-1$
//        if (fieldValue != null) {
//            defaultValue = fieldValue.toString();
//        }
//
//        UI uiHintAnn = field.getAnnotation(UI.class);
//        String uiHint = null;
//        if (uiHintAnn != null) {
//            uiHint = uiHintAnn.value();
//        }
//
//        module.addOutput(fieldName, fieldClass.getCanonicalName(), descriptionStr, defaultValue, uiHint);
//    }
//
//    /**
//     * Get a class from the loaded modules.
//     * 
//     * @param className
//     *            full class name.
//     * @return the class for the given name.
//     * @throws ClassNotFoundException
//     */
//    public Class< ? > getModulesClass( String className ) throws ClassNotFoundException {
//        Class< ? > moduleClass = Class.forName(className, false, jarClassloader);
//        return moduleClass;
//    }
//
//    public InputStream getResourceAsStream( String fullName ) throws IOException {
//        URL resource = jarClassloader.getResource(fullName);
//        InputStream resourceAsStream = resource.openStream();
//        // InputStream resourceAsStream =
//        // jarClassloader.getResourceAsStream(fullName);
//        return resourceAsStream;
//    }
//
//}
