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
package org.hortonmachine.gears;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hortonmachine.gears.libs.modules.ClassField;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

import oms3.Access;
import oms3.ComponentAccess;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

/**
 * Class presenting modules names and classes.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.0
 */
@SuppressWarnings("nls")
public class JGrassGears {
    // private static final Logger logger = LoggerFactory.getLogger(JGrassGears.class);
    /**
     * A {@link LinkedHashMap map} of all the class names and the class itself.
     */
    public final Map<String, Class< ? >> moduleName2Class = new LinkedHashMap<>();

    /**
     * A {@link LinkedHashMap map} of all the class names and their fields.
     */
    public final Map<String, List<ClassField>> moduleName2Fields = new LinkedHashMap<>();

    private static final String PUBLIC_STATIC_FINAL_STRING = "public static final String ";

    private static final String QUOTATION_MARK_SEMICOLON_NEW_LINE = "\";\n";

    private static JGrassGears jgrassGears = null;

    /**
     * An array of all the fields used in the modules.
     */
    private String[] allFields;

    /**
     * An array of all the class names of the modules.
     */
    private String[] allClasses;

    private URL baseclassUrl;

    private JGrassGears( URL baseclassUrl ) {
        this.baseclassUrl = baseclassUrl;
    }

    public String[] getAllFields() {
        return allFields;
    }

    public void setAllFields( String[] allFields ) {
        this.allFields = allFields;
    }

    public String[] getAllClasses() {
        return allClasses;
    }

    public void setAllClasses( String[] allClasses ) {
        this.allClasses = allClasses;
    }

    /**
     * Retrieves the {@link JGrassGears}. If it exists, that instance is returned.
     * 
     * @return the JGrassGears annotations class.
     */
    public static synchronized JGrassGears getInstance() {
        if (jgrassGears == null) {
            jgrassGears = new JGrassGears(null);
            jgrassGears.gatherInformations();
        }
        return jgrassGears;
    }

    /**
     * Retrieves the {@link JGrassGears} for a particular url path.
     * 
     * <p>
     * <b>When this method is called, the {@link JGrassGears} instance is reset.</b>
     * </p>
     * <p>
     * Be careful when you use this. This is a workaround needed for eclipse
     * systems, where the url returned by the urlfinder is a bundleresource that
     * would need to be resolved first with rcp tools we do not want to depend on. 
     * </p>
     * 
     * @return the JGrassGears annotations class.
     */
    public static JGrassGears getInstance( URL baseclassUrl ) {
        jgrassGears = new JGrassGears(baseclassUrl);
        // logger.debug("init JGrassGears modules classes");
        jgrassGears.gatherInformations();
        return jgrassGears;
    }

    private void gatherInformations() {

        try {
            if (baseclassUrl == null) {
                baseclassUrl = ClasspathUrlFinder.findClassBase(JGrassGears.class);
            }

            // logger.debug("base class url: " + baseclassUrl);

            AnnotationDB db = new AnnotationDB();
            db.scanArchives(baseclassUrl);

            Map<String, Set<String>> annotationIndex = db.getAnnotationIndex();
            Set<String> simpleClasses = annotationIndex.get(Execute.class.getName());
            for( String className : simpleClasses ) {
                if (!className.startsWith("org.hortonmachine.gears")) {
                    continue;
                }
                // logger.debug("check: " + className);
                int lastDot = className.lastIndexOf('.');
                String name = className.substring(lastDot + 1);
                Class< ? > clazz = null;
                try {
                    clazz = Class.forName(className);
                    moduleName2Class.put(name, clazz);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            /*
             * extract all classes and fields
             */
            List<String> classNames = new ArrayList<>();
            List<String> fieldNamesList = new ArrayList<>();

            Set<Entry<String, Class< ? >>> moduleName2ClassEntries = moduleName2Class.entrySet();
            for( Entry<String, Class< ? >> moduleName2ClassEntry : moduleName2ClassEntries ) {
                String moduleName = moduleName2ClassEntry.getKey();
                Class< ? > moduleClass = moduleName2ClassEntry.getValue();
                Status annotation = moduleClass.getAnnotation(Status.class);
                if (annotation == null) {
                    System.out.println("Missing status: " + moduleClass.getCanonicalName());
                    continue;
                }
                String statusString;
                int status = annotation.value();
                switch( status ) {
                case Status.CERTIFIED:
                    statusString = "CERTIFIED";
                    break;
                case Status.DRAFT:
                    statusString = "DRAFT";
                    break;
                case Status.TESTED:
                    statusString = "TESTED";
                    break;
                default:
                    statusString = "UNKNOWN";
                    break;
                }

                classNames.add(moduleName);

                List<ClassField> tmpfields = new ArrayList<>();
                Object annotatedObject = moduleClass.newInstance();
                ComponentAccess cA = new ComponentAccess(annotatedObject);
                Collection<Access> inputs = cA.inputs();
                for( Access access : inputs ) {
                    Field field = access.getField();
                    String name = field.getName();
                    Description descriptionAnnot = field.getAnnotation(Description.class);
                    String description = name;
                    if (descriptionAnnot != null) {
                        description = descriptionAnnot.value();
                        if (description == null) {
                            description = name;
                        }
                    }
                    Class< ? > fieldClass = field.getType();
                    ClassField cf = new ClassField();
                    cf.isIn = true;
                    cf.fieldName = name;
                    cf.fieldDescription = description;
                    cf.fieldClass = fieldClass;
                    cf.parentClass = moduleClass;
                    cf.parentClassStatus = statusString;
                    if (!fieldNamesList.contains(name)) {
                        fieldNamesList.add(name);
                    }
                    tmpfields.add(cf);

                }
                Collection<Access> outputs = cA.outputs();
                for( Access access : outputs ) {
                    Field field = access.getField();
                    String name = field.getName();
                    Description descriptionAnnot = field.getAnnotation(Description.class);
                    String description = name;
                    if (descriptionAnnot != null) {
                        description = descriptionAnnot.value();
                        if (description == null) {
                            description = name;
                        }
                    }
                    Class< ? > fieldClass = field.getType();
                    ClassField cf = new ClassField();
                    cf.isOut = true;
                    cf.fieldName = name;
                    cf.fieldDescription = description;
                    cf.fieldClass = fieldClass;
                    cf.parentClass = moduleClass;
                    cf.parentClassStatus = statusString;
                    if (!fieldNamesList.contains(name)) {
                        fieldNamesList.add(name);
                    }
                    tmpfields.add(cf);
                }
                moduleName2Fields.put(moduleName, tmpfields);
            }
            Collections.sort(fieldNamesList);
            allFields = (String[]) fieldNamesList.toArray(new String[fieldNamesList.size()]);
            Collections.sort(classNames);
            allClasses = (String[]) classNames.toArray(new String[classNames.size()]);
        } catch (InstantiationException | IllegalAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void main( String[] args ) throws IOException {
        JGrassGears jgr = getInstance();

        Set<Entry<String, Class< ? >>> cls = jgr.moduleName2Class.entrySet();
        for( Entry<String, Class< ? >> cl : cls ) {
            System.out.println(cl.getValue().getCanonicalName());
        }
        Map<String, List<ClassField>> moduleName2Fields = jgr.moduleName2Fields;
        Map<String, Class< ? >> moduleName2Class = jgr.moduleName2Class;

        Set<Entry<String, List<ClassField>>> entrySet = moduleName2Fields.entrySet();
        for( Entry<String, List<ClassField>> entry : entrySet ) {
            String moduleName = entry.getKey();

            StringBuilder sb = new StringBuilder();

            Class< ? > moduleClass = moduleName2Class.get(moduleName);
            Description description = moduleClass.getAnnotation(Description.class);
            sb.append(PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_DESCRIPTION = \"" + description.value()
                    + QUOTATION_MARK_SEMICOLON_NEW_LINE);
            Documentation documentation = moduleClass.getAnnotation(Documentation.class);
            String doc;
            if (documentation == null) {
                doc = "";
            } else {
                doc = documentation.value();
            }
            sb.append(PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_DOCUMENTATION = \"" + doc
                    + QUOTATION_MARK_SEMICOLON_NEW_LINE);
            Keywords keywords = moduleClass.getAnnotation(Keywords.class);
            String k;
            if (keywords == null) {
                k = "";
            } else {
                k = keywords.value();
            }
            sb.append(PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_KEYWORDS = \"" + k
                    + QUOTATION_MARK_SEMICOLON_NEW_LINE);
            Label label = moduleClass.getAnnotation(Label.class);
            String lab;
            if (label == null) {
                lab = "";
            } else {
                lab = label.value();
            }
            sb.append(PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_LABEL = \"" + lab
                    + QUOTATION_MARK_SEMICOLON_NEW_LINE);
            Name name = moduleClass.getAnnotation(Name.class);
            String n;
            if (name == null) {
                n = "";
            } else {
                n = name.value();
            }
            sb.append(
                    PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_NAME = \"" + n + QUOTATION_MARK_SEMICOLON_NEW_LINE);
            Status status = moduleClass.getAnnotation(Status.class);

            sb.append("public static final int " + moduleName.toUpperCase() + "_STATUS = " + status.value() + ";\n");
            License license = moduleClass.getAnnotation(License.class);
            sb.append(PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_LICENSE = \"" + license.value()
                    + QUOTATION_MARK_SEMICOLON_NEW_LINE);
            Author author = moduleClass.getAnnotation(Author.class);
            String authorName = author.name();
            sb.append(PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_AUTHORNAMES = \"" + authorName
                    + QUOTATION_MARK_SEMICOLON_NEW_LINE);
            String authorContact = author.contact();
            sb.append(PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_AUTHORCONTACTS = \"" + authorContact
                    + QUOTATION_MARK_SEMICOLON_NEW_LINE);

            UI ui = moduleClass.getAnnotation(UI.class);
            if (ui != null) {
                sb.append(PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_UI = \"" + ui.value()
                        + QUOTATION_MARK_SEMICOLON_NEW_LINE);
            }

            List<ClassField> value = entry.getValue();
            for( ClassField classField : value ) {
                String fieldName = classField.fieldName;
                if (fieldName.equals("pm")) {
                    continue;
                }
                String fieldDescription = classField.fieldDescription;

                String str = PUBLIC_STATIC_FINAL_STRING + moduleName.toUpperCase() + "_" + fieldName + "_DESCRIPTION = \""
                        + fieldDescription + QUOTATION_MARK_SEMICOLON_NEW_LINE;
                sb.append(str);
            }
            System.out.println(sb.toString());
            System.out.println();
        }
    }

}
