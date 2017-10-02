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

import java.lang.reflect.Method;
import java.util.Locale;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

/**
 * Utilities to handle annotations.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class AnnotationUtilities {

    private static String LANG = Locale.getDefault().getCountry().toLowerCase();

    /**
     * Gets the localized description of the {@link Description}.
     * 
     * @param description the {@link Description} annotation.
     * @return the description string or " - ".
     * @throws Exception
     */
    public static String getLocalizedDescription( Description description ) throws Exception {
        // try to get the language
        Class< ? > annotationclass = Description.class;
        return getLocalizedString(description, annotationclass);
    }

    public static String getLocalizedDocumentation( Documentation documentation ) throws Exception {
        // try to get the language
        Class< ? > annotationclass = Documentation.class;
        return getLocalizedString(documentation, annotationclass);
    }

    public static String getLocalizedStatus( Status annotation ) throws Exception {
        // try to get the language
        Class< ? > annotationclass = Status.class;
        return getLocalizedString(annotation, annotationclass);
    }

    public static String getLocalizedName( Name annotation ) throws Exception {
        // try to get the language
        Class< ? > annotationclass = Name.class;
        return getLocalizedString(annotation, annotationclass);
    }

    public static String getLocalizedAuthor( Author annotation ) throws Exception {
        // try to get the language
        Class< ? > annotationclass = Author.class;
        return getLocalizedString(annotation, annotationclass);
    }
    public static String getLocalizedLicense( License annotation ) throws Exception {
        // try to get the language
        Class< ? > annotationclass = License.class;
        return getLocalizedString(annotation, annotationclass);
    }
    public static String getLocalizedKeywords( Keywords annotation ) throws Exception {
        // try to get the language
        Class< ? > annotationclass = Keywords.class;
        return getLocalizedString(annotation, annotationclass);
    }

    private static String getLocalizedString( Object object, Class< ? > annotationclass ) throws Exception {
        Method method = null;
        try {
            method = annotationclass.getMethod(LANG);
        } catch (Exception e) {
            // ignore
        }
        String valueStr = "value";
        if (method == null) {
            try {
                method = annotationclass.getMethod(valueStr);
            } catch (Exception e) {
                // ignore
            }
        }

        if (method != null) {
            try {
                Object result = method.invoke(object);
                if (result instanceof String) {
                    String descriptionStr = (String) result;
                    if (descriptionStr.length() > 0) {
                        return descriptionStr;
                    } else {
                        // the method of the language exists but was not filled
                        // try with value()
                        try {
                            method = annotationclass.getMethod(valueStr);
                            result = method.invoke(object);
                            if (result instanceof String) {
                                descriptionStr = (String) result;
                                if (descriptionStr.length() > 0) {
                                    return descriptionStr;
                                }
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            } catch (Exception e) {
                // ignore and return default
            }
        }

        return " - ";
    }

}
