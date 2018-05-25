package org.hortonmachine.modules;

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

import org.hortonmachine.gears.JGrassGears;
import org.hortonmachine.gears.libs.modules.ClassField;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;

public class Modules {

    private static Modules modules = null;

    private URL baseclassUrl;
    private Modules( URL baseclassUrl ) {
        this.baseclassUrl = baseclassUrl;
    }

    /**
     * Retrieves the {@link Modules}. If it exists, that instance is returned.
     * 
     * @return the Modules annotations class.
     */
    public synchronized static Modules getInstance() {
        if (modules == null) {
            modules = new Modules(null);
            modules.gatherInformations();
        }
        return modules;
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
    public static Modules getInstance( URL baseclassUrl ) {
        modules = new Modules(baseclassUrl);
        modules.gatherInformations();
        return modules;
    }

    /**
     * A {@link LinkedHashMap map} of all the class names and the class itself.
     */
    public final LinkedHashMap<String, Class< ? >> moduleName2Class = new LinkedHashMap<String, Class< ? >>();

    /**
     * A {@link LinkedHashMap map} of all the class names and their fields.
     */
    public final LinkedHashMap<String, List<ClassField>> moduleName2Fields = new LinkedHashMap<String, List<ClassField>>();

    /**
     * An array of all the fields used in the modules.
     */
    public String[] allFields = null;

    /**
     * An array of all the class names of the modules.
     */
    public String[] allClasses = null;

    private void gatherInformations() {

        try {
            if (baseclassUrl == null) {
                baseclassUrl = ClasspathUrlFinder.findClassBase(Modules.class);
            }
            AnnotationDB db = new AnnotationDB();
            db.scanArchives(baseclassUrl);

            Map<String, Set<String>> annotationIndex = db.getAnnotationIndex();
            Set<String> simpleClasses = annotationIndex.get(Execute.class.getName());
            for( String className : simpleClasses ) {
                if (!className.startsWith("org.hortonmachine.modules")) {
                    continue;
                }
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
            List<String> classNames = new ArrayList<String>();
            List<String> fieldNamesList = new ArrayList<String>();

            Set<Entry<String, Class< ? >>> moduleName2ClassEntries = moduleName2Class.entrySet();
            for( Entry<String, Class< ? >> moduleName2ClassEntry : moduleName2ClassEntries ) {
                String moduleName = moduleName2ClassEntry.getKey();
                Class< ? > moduleClass = moduleName2ClassEntry.getValue();
                Status annotation = moduleClass.getAnnotation(Status.class);
                if (annotation == null) {
                    System.out.println("Missing status: " + moduleClass.getCanonicalName());
                    continue;
                }
                String statusString = null;
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

                List<ClassField> tmpfields = new ArrayList<ClassField>();
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
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main( String[] args ) throws IOException {
        Modules mod = getInstance();

        Set<Entry<String, Class< ? >>> cls = mod.moduleName2Class.entrySet();
        for( Entry<String, Class< ? >> cl : cls ) {
            System.out.println(cl.getValue().getCanonicalName());
        }
        if (true)
            return;
        LinkedHashMap<String, List<ClassField>> moduleName2Fields = mod.moduleName2Fields;
        LinkedHashMap<String, Class< ? >> moduleName2Class = mod.moduleName2Class;

        Set<Entry<String, List<ClassField>>> entrySet = moduleName2Fields.entrySet();
        for( Entry<String, List<ClassField>> entry : entrySet ) {
            String moduleName = entry.getKey();

            StringBuilder sb = new StringBuilder();

            Class< ? > moduleClass = moduleName2Class.get(moduleName);
            Description description = moduleClass.getAnnotation(Description.class);
            sb.append("public static final String " + moduleName.toUpperCase() + "_DESCRIPTION = \"" + description.value()
                    + "\";\n");
            Documentation documentation = moduleClass.getAnnotation(Documentation.class);
            String doc;
            if (documentation == null) {
                doc = "";
            } else {
                doc = documentation.value();
            }
            sb.append("public static final String " + moduleName.toUpperCase() + "_DOCUMENTATION = \"" + doc + "\";\n");
            Keywords keywords = moduleClass.getAnnotation(Keywords.class);
            String k;
            if (keywords == null) {
                k = "";
            } else {
                k = keywords.value();
            }
            sb.append("public static final String " + moduleName.toUpperCase() + "_KEYWORDS = \"" + k + "\";\n");
            Label label = moduleClass.getAnnotation(Label.class);
            String lab;
            if (label == null) {
                lab = "";
            } else {
                lab = label.value();
            }
            sb.append("public static final String " + moduleName.toUpperCase() + "_LABEL = \"" + lab + "\";\n");
            Name name = moduleClass.getAnnotation(Name.class);
            String n;
            if (name == null) {
                n = "";
            } else {
                n = name.value();
            }
            sb.append("public static final String " + moduleName.toUpperCase() + "_NAME = \"" + n + "\";\n");
            Status status = moduleClass.getAnnotation(Status.class);
            // String st = "";
            // switch( status.value() ) {
            // case 5:
            // st = "EXPERIMENTAL";
            // break;
            // case 10:
            // st = "DRAFT";
            // break;
            // case 20:
            // st = "TESTED";
            // break;
            // case 30:
            // st = "VALIDATED";
            // break;
            // case 40:
            // st = "CERTIFIED";
            // break;
            // default:
            // st = "DRAFT";
            // break;
            // }

            sb.append("public static final int " + moduleName.toUpperCase() + "_STATUS = " + status.value() + ";\n");
            License license = moduleClass.getAnnotation(License.class);
            sb.append("public static final String " + moduleName.toUpperCase() + "_LICENSE = \"" + license.value() + "\";\n");
            Author author = moduleClass.getAnnotation(Author.class);
            String authorName = author.name();
            sb.append("public static final String " + moduleName.toUpperCase() + "_AUTHORNAMES = \"" + authorName + "\";\n");
            String authorContact = author.contact();
            sb.append("public static final String " + moduleName.toUpperCase() + "_AUTHORCONTACTS = \"" + authorContact + "\";\n");

            UI ui = moduleClass.getAnnotation(UI.class);
            if (ui != null) {
                sb.append("public static final String " + moduleName.toUpperCase() + "_UI = \"" + ui.value() + "\";\n");
            }

            List<ClassField> value = entry.getValue();
            for( ClassField classField : value ) {
                String fieldName = classField.fieldName;
                if (fieldName.equals("pm")) {
                    continue;
                }
                String fieldDescription = classField.fieldDescription;

                String str = "public static final String " + moduleName.toUpperCase() + "_" + fieldName + "_DESCRIPTION = \""
                        + fieldDescription + "\";\n";
                sb.append(str);
            }
            System.out.println(sb.toString());
            System.out.println();
        }

        // for( String className : jgr.allClasses ) {
        // System.out.println(className);
        // }
        // for( String fieldName : jgr.allFields ) {
        // System.out.println(fieldName);
        // }
    }

    // public static void main( String[] args ) {
    // ServiceLoader<HMModel> load = ServiceLoader.load(HMModel.class);
    // for( HMModel omsSpi : load ) {
    // System.out.println(omsSpi);
    // }
    // }

}
