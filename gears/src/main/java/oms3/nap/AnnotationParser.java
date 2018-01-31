/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.nap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oms3.ComponentException;

/** Annotation Processor.
 *
 * @author od
 */
public class AnnotationParser {

//    static private Pattern annPattern = Pattern.compile  ("@(\\w+)(\\s*\\(([^\\)]*)\\))?\\n", Pattern.DOTALL);
    static private Pattern annPattern = Pattern.compile("@(\\w+)(\\s*\\(([^\\)]*)\\))?[^\\n]*\\n", Pattern.DOTALL);
    static private Pattern annTestPattern = Pattern.compile("@(\\w+)", Pattern.MULTILINE);

    private AnnotationParser() {
    }

    /**
     * Handle a file with an annotation handler.
     * 
     * @param file
     * @param ah
     * @throws java.lang.Exception 
     */
    public static void handle(File srcFile, AnnotationHandler ah) throws Exception {
        FileInputStream fis = new FileInputStream(srcFile);
        FileChannel fc = fis.getChannel();

        // Get a CharBuffer from the source file
        ByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        CharsetDecoder cd = Charset.forName("8859_1").newDecoder();
        CharBuffer cb = cd.decode(bb);

        // handle the content.
        ah.start(cb.toString());
        handle(cb.toString(), ah);
        ah.done();

        fis.close();
    }

    /**
     * Handle a string with an annotation handler
     * @param s the String to process
     * @param ah the annotation handler to use.
     */
    public static void handle(String s, AnnotationHandler ah) {
        Map<String, Map<String, String>> l = new LinkedHashMap<String, Map<String, String>>();
        Matcher m = annPattern.matcher(s);
        while (m.find()) {
//            for (int i = 1; i <= m.groupCount(); i++) {
//                System.out.println("Group " + i + " '" + m.group(i) + "'");
//            }

            String rest = s.substring(m.end(0)).trim();
            String srcLine = null;
            if (rest.indexOf('\n') > -1) {
                srcLine = rest.substring(0, rest.indexOf('\n'));
            } else {
                srcLine = rest;
            }

            Map<String, String> val = new LinkedHashMap<String, String>();
            String annArgs = m.group(3);
            if (annArgs == null) {
                // no annotations arguments
                // e.g   '@Function'
            } else if (annArgs.indexOf('=') > -1) {
                // KVP annotation
                // e.g.  '@Function(name="test", scope="global")'
                StringTokenizer t = new StringTokenizer(annArgs, ",");
                while (t.hasMoreTokens()) {
                    String arg = t.nextToken();
                    String key = arg.substring(0, arg.indexOf('='));
                    String value = arg.substring(arg.indexOf('=') + 1);
                    val.put(key.trim(), value.trim());
                }
            } else {
                // single value annotation
                // e.g.  '@Function("test");
                val.put(AnnotationHandler.VALUE, annArgs);
            }

            l.put(m.group(1), val);

            // If the next line also has an annotation
            // no source line will be passed into the handler
            if (!annTestPattern.matcher(srcLine).find()) {
                ah.handle(l, srcLine);
                ah.log(" Ann -> " + l);
                ah.log(" Src -> " + srcLine);
                l = new LinkedHashMap<String, Map<String, String>>();
            }
        }
    }

    /**
     * Trims the string quotes, if the argument is a java/c string
     * @param val the string process for quotes
     * @return the same string without quotes, or the same if there are not
     *         any.
     */
    public static String trimQuotes(String val) {
        if ((val.charAt(0) == '"') && (val.charAt(val.length() - 1) == '"')) {
            return val.substring(1, val.length() - 1);
        }
        return val;
    }

    /**
     * Annotation map back to String.
     * @param ann
     * @return
     */
    static String toString(Map<String, Map<String, String>> ann) {
        StringBuilder b = new StringBuilder();
        for (String decl : ann.keySet()) {
            b.append(" @" + decl);
            Map<String, String> v = ann.get(decl);
            String value = v.get(AnnotationHandler.VALUE);
            if (value != null) {
                b.append(" (" + value + ")");
            } else if (v.size() > 0) {
                b.append(" (");
                int i = -1;
                for (String string : v.keySet()) {
                    i++;
                    b.append(string + "=" + v.get(string));
                    if (i < v.size() - 1) {
                        b.append(", ");
                    }
                }
                b.append(")");
            }
            b.append("\n");
        }
        return b.toString();
    }

    //    CHARACTER(kind=C_CHAR, len=hyd2er_len)
//    static Map<String, String> getDeclModifier(String decl) {
//        String p = decl.substring(decl.indexOf('(') + 1, decl.lastIndexOf(')')).trim();
//        String[] kvpl = p.split("\\s*,\\s*");
//        Map<String, String> map = new HashMap<String, String>();
//        for (String string : kvpl) {
//            String[] kvp = string.split("\\s*=\\s*");
//            map.put(kvp[0], kvp.length > 1 ? kvp[1] : null);
//        }
//        return map;
//    }
//    /**
//     * Derives a new File name from an existing one in the same parent
//     * folder
//     *
//     * @param file     the original file
//     * @param prefix   the name prefix
//     * @param postfix  the file name postfix
//     * @param ext      the new extension
//     * @return         the new file
//     */
//    public static File newFileName(File file, String prefix, String postfix, String ext) {
//        String name = file.getName();
//        return new File(file.getParentFile(), prefix
//                + name.substring(0, name.indexOf(".")) + postfix + '.' + ext);
//    }
}
