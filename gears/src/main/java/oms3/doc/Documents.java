/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.doc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

import oms3.annotations.*;
import oms3.io.CSProperties;
import oms3.util.Components;

/** Documenting a Model.
 *
 * @author od
 */
public class Documents {

    /** Document 
     * 
     * @param file the xml outputfile.
     * @param comp the component to document
     * @param params the model parameter
     * @param title the title
     * @throws FileNotFoundException
     */
    public static void db5Sim(File file, Class comp, CSProperties params, String title) throws FileNotFoundException {
        db5Sim(file, comp, params, title, Locale.getDefault());
    }

    public static void db5Sim(File file, Class comp, CSProperties params, String title, Locale loc) throws FileNotFoundException {
        DB5 db5 = new DB5(loc);
        db5.generateDB5(file, comp, params, title);
    }

    /**
     * 
     */
    static class DB5 {

        private static String db50 = "<book version='5.0' xmlns='http://docbook.org/ns/docbook'"
                + " xmlns:xlink='http://www.w3.org/1999/xlink'"
                + " xmlns:xi='http://www.w3.org/2001/XInclude'"
                + " xmlns:svg='http://www.w3.org/2000/svg'"
                + " xmlns:m='http://www.w3.org/1998/Math/MathML'"
                + " xmlns:html='http://www.w3.org/1999/xhtml'"
                + " xmlns:db='http://docbook.org/ns/docbook'>";
        Locale loc;
        ResourceBundle b;

        DB5(Locale loc) {
            this.loc = loc;
            b = ResourceBundle.getBundle("oms3.doc.Loc", loc);
        }

        void generateDB5(File file, Class comp, CSProperties params, String title) throws FileNotFoundException {
            SimpleDateFormat df = new SimpleDateFormat(b.getString("date_format"), loc);

            PrintStream f = new PrintStream(file);
            f.println(db50);
            f.println("<info>");
            f.println("<title>" + title + "</title>");
            f.println("<subtitle>" + b.getString("subtitle") + "</subtitle>");
            f.println("<pubdate>" + df.format(new Date()) + "</pubdate>");
            f.println("</info>");
            if (comp != null) {
                Collection<Class<?>> c = Components.internalComponents(comp);
                f.println(getComponentsChapter(c));
            }
            if (params != null) {
                f.println(getParamChapter(params));
            }
            f.println("<index/>");
            f.println("</book>");
            f.close();
        }

        String getParamChapter(CSProperties p) {
            StringBuffer db = new StringBuffer();
            db.append("<chapter>");
            db.append("<title>" + b.getString("parameterset") + "</title>");
            db.append(paramList("Parameter", p));
            db.append("</chapter>");
            return db.toString();
        }

        String paramList(String title, CSProperties p) {
            if (p.size() == 0) {
                return "";
            }

            List<String> keys = new ArrayList<String>(p.keySet());
            Collections.sort(keys);

            StringBuffer db = new StringBuffer();
            db.append("<variablelist>");
            for (String name : keys) {
                db.append(varlistentry(name, p.get(name).toString(), p.getInfo(name)));
            }
            db.append("</variablelist>");
            return db.toString();
        }

        String varlistentry(String name, String item, Map<String, String> info) {
            StringBuffer db = new StringBuffer();
            db.append("<varlistentry><term><emphasis role='bold'>" + name + "</emphasis>");
            db.append("<indexterm><primary>" + name + " (" + b.getString("parameter") + ")</primary><secondary>"
                    + "Value</secondary></indexterm>");

            String descr = info.get("descr");
            if (descr != null) {
                db.append(" - " + descr);
            }
            db.append("</term>");
            db.append("<listitem><para><code>");
            db.append(item);
            db.append("</code></para>");
            for (String key : info.keySet()) {
                if (!key.equals("descr")) {
                    db.append("<para>" + key + " - " + info.get(key) + "</para>");
                }
            }
            db.append("</listitem>");
            db.append("</varlistentry>");
            return db.toString();
        }

        String classSection(Class<?> c) {
            StringBuffer db = new StringBuffer();
            db.append("<section>");
            db.append("<title>" + b.getString("component") + " '" + c.getSimpleName() + "'</title>");
            db.append("<indexterm><primary>" + c.getSimpleName() + " (" + b.getString("component") + ")</primary></indexterm>");

            Keywords keywords = (Keywords) c.getAnnotation(Keywords.class);
            if (keywords != null && !keywords.value().isEmpty()) {
                StringTokenizer t = new StringTokenizer(keywords.value(), ",");
                while (t.hasMoreTokens()) {
                    db.append("<indexterm><primary>" + t.nextToken().trim() + " (" + b.getString("keyword") + ")</primary><secondary>"
                            + c.getSimpleName() + "</secondary></indexterm>");
                }
            }

            Description descr = (Description) c.getAnnotation(Description.class);
            if (descr != null) {
                String d = locDesc(descr, loc);
                db.append("<para><![CDATA[" + d + "]]></para>");
            }
            db.append("<variablelist>");
            db.append(varlistentry(b.getString("name"), "<code>" + c.getName() + "</code>"));
            Author author = (Author) c.getAnnotation(Author.class);
            if (author != null) {
                StringTokenizer t = new StringTokenizer(author.name(), ",");
                StringBuffer authindex = new StringBuffer();
                while (t.hasMoreTokens()) {
                    authindex.append("<indexterm><primary>" + t.nextToken().trim() + " (" + b.getString("author") + ")</primary><secondary>"
                            + c.getSimpleName() + "</secondary></indexterm>");
                }
                db.append(varlistentry(b.getString("author"), author.name() + (author.contact().isEmpty() ? "" : " - " + author.contact()),
                        authindex.toString()));

            }
            if (keywords != null && !keywords.value().isEmpty()) {
                db.append(varlistentry(b.getString("keyword"), keywords.value()));
            }
            VersionInfo version = (VersionInfo) c.getAnnotation(VersionInfo.class);
            if (version != null) {
                db.append(varlistentry(b.getString("version"), version.value()));
            }
            SourceInfo source = (SourceInfo) c.getAnnotation(SourceInfo.class);
            if (source != null) {
                String src = source.value();
//                    if (src.startsWith("$") && src.endsWith("$")) {
//                        src = src.substring(src.indexOf(' ') + 1, src.lastIndexOf(' '));
//                    }
                db.append(varlistentry(b.getString("source"), src));
            }
            License lic = (License) c.getAnnotation(License.class);
            if (lic != null) {
                db.append(varlistentry(b.getString("license"), lic.value()));
            }
            db.append("</variablelist>");

            db.append(table(b.getString("parameter"), b.getString("parameter"), c, Components.parameter(c)));
            db.append(table(b.getString("var_in"), b.getString("variable"), c, Components.inVars(c)));
            db.append(table(b.getString("var_out"), b.getString("variable"), c, Components.outVars(c)));

            Bibliography biblio = (Bibliography) c.getAnnotation(Bibliography.class);
            if (biblio != null) {
                db.append("<section>");
                db.append("<title>" + b.getString("bibliography")+ "</title>");
                db.append("<variablelist>");
                StringBuffer bs = new StringBuffer("<itemizedlist>");
                for (String entry : biblio.value()) {
                    bs.append("<listitem><para>" + entry + "</para></listitem>");
                }
                bs.append("</itemizedlist>");
                db.append(varlistentry("", bs.toString()));
                db.append("</variablelist>");
                db.append("</section>");
            }
            Documentation doc = (Documentation) c.getAnnotation(Documentation.class);
            if (doc != null) {
                String v = doc.value();
                if (v.endsWith(".xml")) {
                    try {
                        URL url = new URL(v);
                        db.append("<xi:include href='" + v + "' xmlns:xi='http://www.w3.org/2001/XInclude'/>");
                    } catch (MalformedURLException E) {
                        try {
                            File rel = new File(v);
                            // relative reference "src/...."
                            if (!rel.isAbsolute() && System.getProperty("oms3.work") != null) {
                                File work = new File(System.getProperty("oms3.work"));
                                File locF = locFile(work, v, loc);
                                v = ("file:" + locF.toString()).replace('\\', '/');
                            }
                            File f = new File(new URI(v));
                            if (f.exists()) {
                                db.append("<xi:include href='" + v + "' xmlns:xi='http://www.w3.org/2001/XInclude'/>");
                            } else {
                                System.out.println("Document not found: " + v);
                            }
                        } catch (URISyntaxException ex) {
                            System.out.println("Document not found: " + v);
                        } catch (IllegalArgumentException iae) {
                            iae.printStackTrace(System.out);
                            System.out.println("Illegal Argument: " + iae.getMessage() + " " + v);
                        }
                    }
                } else {
                    db.append("<variablelist><varlistentry><term><emphasis role='bold'>" + b.getString("further") + "</emphasis></term>");
                    db.append("<listitem><para>");
                    db.append(v);
                    db.append("</para></listitem>");
                    db.append("</varlistentry></variablelist>");
                }
            }
            db.append("</section>");
            return db.toString();
        }

        @SuppressWarnings("unchecked")
        String getComponentsChapter(Collection<Class<?>> comps) {

            StringBuffer db = new StringBuffer();

            // Model component
            db.append("<chapter>");
            db.append("<title>" + b.getString("model") + "</title>");
            Class mai = comps.iterator().next();  // the first component is top.
            db.append(classSection(mai));
            db.append("</chapter>");

            comps.remove(mai);      // remove the main component
            Map<Package, List<Class<?>>> pmap = categorize(comps);
            List<Package> pl = new ArrayList<Package>(pmap.keySet());
            Collections.sort(pl, new Comparator<Package>() {

                @Override
                public int compare(Package o1, Package o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });

            // Subcomponents.
            db.append("<chapter>");
            db.append("<title>" + b.getString("sub") + "</title>");
            for (Package p : pl) {
                db.append("<section>");
                db.append("<title>'" + p.getName() + "'</title>");
                List<Class<?>> co = pmap.get(p);
                Collections.sort(co, new Comparator<Class<?>>() {

                    @Override
                    public int compare(Class o1, Class o2) {
                        return o1.getSimpleName().compareToIgnoreCase(o2.getSimpleName());
                    }
                });
                for (Class c : co) {
                    db.append(classSection(c));
                }
                db.append("</section>");
            }
            db.append("</chapter>");
            return db.toString();
        }

        String table(String title, String cl, Class comp, List<Field> l) {
            if (l.size() == 0) {
                return "";
            }

            Collections.sort(l, new Comparator<Field>() {

                @Override
                public int compare(Field o1, Field o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });

            StringBuffer db = new StringBuffer();
            db.append("<section>");
            db.append("<title>" + title + "</title>");
            db.append("<variablelist>");
            for (Field field : l) {
                db.append("<varlistentry><term><emphasis role='bold'>" + field.getName() + "</emphasis>");
                Unit unit = field.getAnnotation(Unit.class);
                if (unit != null) {
                    db.append("<code> [");
                    db.append(unit.value());
                    db.append("]</code>");
                }

                db.append("<code> - " + field.getType().getSimpleName() + "</code>");

                Range range = field.getAnnotation(Range.class);
                if (range != null) {
                    db.append("  (");
                    db.append(range.min() == Double.MIN_VALUE ? "" : range.min());
                    db.append(" ... ");
                    db.append(range.max() == Double.MAX_VALUE ? "" : range.max());
                    db.append(")");
                }

                db.append("<indexterm><primary>" + field.getName() + " (" + cl + ")</primary><secondary>"
                        + field.getDeclaringClass().getSimpleName() + "</secondary></indexterm>");
                db.append("</term>");
                db.append("<listitem><para>");
                Description descr = field.getAnnotation(Description.class);
                if (descr != null) {
                    String d = locDesc(descr, loc);
                    db.append("<![CDATA[" + d + "]]>");
                }
                db.append("</para></listitem>");
                db.append("</varlistentry>");
            }

            db.append("</variablelist>");
            db.append("</section>");
            return db.toString();
        }

        static String varlistentry(String name, String item) {
            return varlistentry(name, item, "");
        }

        static String varlistentry(String name, String item, String indexterm) {
            StringBuffer db = new StringBuffer();
            db.append("<varlistentry><term><emphasis role='bold'>" + name + "</emphasis>" + indexterm + "</term>");
            db.append("<listitem><para>");
            db.append(item);
            db.append("</para></listitem>");
            db.append("</varlistentry>");
            return db.toString();
        }

        static Map<Package, List<Class<?>>> categorize(Collection<Class<?>> comp) {
            Map<Package, List<Class<?>>> packages = new HashMap<Package, List<Class<?>>>();
            for (Class<?> c : comp) {
                Package p = c.getPackage();
                List<Class<?>> tos = packages.get(p);
                if (tos == null) {
                    tos = new ArrayList<Class<?>>();
                    packages.put(p, tos);
                }
                tos.add(c);
            }
            return packages;
        }

        static File locFile(File work, String descr, Locale l) {
            if (l == null) {
                return new File(work, descr);
            }

            String locName = descr.substring(0, descr.lastIndexOf('.'));
            String locExt = descr.substring(descr.lastIndexOf('.'));
            String locLan = l.getLanguage();

            File f = new File(work, locName + "_" + locLan + locExt);
            if (f.exists()) {
                return f;
            }

            f = new File(work, descr);
            if (f.exists()) {
                return f;
            }
            throw new IllegalArgumentException(descr);
        }

        static String locDesc(Description descr, Locale l) {
            if (l == null) {
                return descr.value();
            }
            if (l.getLanguage().equals("en")) {
                return descr.en().isEmpty() ? descr.value() : descr.en();
            } else if (l.getLanguage().equals("de")) {
                return descr.de().isEmpty() ? descr.value() : descr.de();
            }
            return descr.value();
        }
    }
}
