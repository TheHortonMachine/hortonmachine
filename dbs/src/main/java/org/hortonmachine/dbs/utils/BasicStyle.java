/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
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
package org.hortonmachine.dbs.utils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONObject;

/**
 * Simple style for shapes.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class BasicStyle implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ID = "_id";
    public static final String NAME = "name";
    public static final String SIZE = "size";
    public static final String FILLCOLOR = "fillcolor";
    public static final String STROKECOLOR = "strokecolor";
    public static final String FILLALPHA = "fillalpha";
    public static final String STROKEALPHA = "strokealpha";
    public static final String SHAPE = "shape";
    public static final String WIDTH = "width";
    public static final String ENABLED = "enabled";
    public static final String ORDER = "layerorder";
    public static final String DECIMATION = "decimationfactor";
    public static final String DASH = "dashpattern";
    public static final String MINZOOM = "minzoom";
    public static final String MAXZOOM = "maxzoom";
    public static final String LABELFIELD = "labelfield";
    public static final String LABELSIZE = "labelsize";
    public static final String LABELVISIBLE = "labelvisible";
    public static final String UNIQUEVALUES = "uniquevalues";
    public static final String THEME = "theme";

    public long id;
    public String name;
    public double size = 5;
    public String fillcolor = "red";
    public String strokecolor = "black";
    public double fillalpha = 0.3;
    public double strokealpha = 1.0;
    /**
     * WKT shape name.
     */
    public String shape = "square";
    /**
     * The stroke width.
     */
    public double width = 3;
    /**
     * The text size.
     */
    public double labelsize = 15;

    /**
     * Field to use for labeling.
     */
    public String labelfield = "";
    /**
     * Defines if the labeling is enabled.
     * <p/>
     * <ul>
     * <li>0 = false</li>
     * <li>1 = true</li>
     * </ul>
     */
    public int labelvisible = 0;

    /**
     * Defines if the layer is enabled.
     * <p/>
     * <ul>
     * <li>0 = false</li>
     * <li>1 = true</li>
     * </ul>
     */
    public int enabled = 0;
    /**
     * Vertical order of the layer.
     */
    public int order = 0;
    /**
     * The pattern to dash lines.
     * <p/>
     * <p>The format is an array of floats, the first number being the shift.
     */
    public String dashPattern = "";
    /**
     * Min possible zoom level.
     */
    public int minZoom = 0;
    /**
     * Max possible zoom level.
     */
    public int maxZoom = 22;
    /**
     * Decimation factor for geometries.
     */
    public double decimationFactor = 0.0;

    /**
     * If a unique style is defined, the hashmap contains in key the unique value
     * and in value the style to apply. 
     */
    public HashMap<String, BasicStyle> themeMap;

    public String themeField;

    public BasicStyle duplicate() {
        BasicStyle dup = new BasicStyle();
        dup.id = id;
        dup.name = name;
        dup.size = size;
        dup.fillcolor = fillcolor;
        dup.strokecolor = strokecolor;
        dup.fillalpha = fillalpha;
        dup.strokealpha = strokealpha;
        dup.shape = shape;
        dup.width = width;
        dup.labelsize = labelsize;
        dup.labelfield = labelfield;
        dup.labelvisible = labelvisible;
        dup.enabled = enabled;
        dup.order = order;
        dup.dashPattern = dashPattern;
        dup.minZoom = minZoom;
        dup.maxZoom = maxZoom;
        dup.decimationFactor = decimationFactor;
        dup.themeMap = themeMap;
        return dup;
    }

    /**
     * @return a string that can be used in a sql insert statement with
     * all the values placed.
     */
    public String insertValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'");
        sb.append(name);
        sb.append("', ");
        sb.append(size);
        sb.append(", '");
        sb.append(fillcolor);
        sb.append("', '");
        sb.append(strokecolor);
        sb.append("', ");
        sb.append(fillalpha);
        sb.append(", ");
        sb.append(strokealpha);
        sb.append(", '");
        sb.append(shape);
        sb.append("', ");
        sb.append(width);
        sb.append(", ");
        sb.append(labelsize);
        sb.append(", '");
        sb.append(labelfield);
        sb.append("', ");
        sb.append(labelvisible);
        sb.append(", ");
        sb.append(enabled);
        sb.append(", ");
        sb.append(order);
        sb.append(", '");
        sb.append(dashPattern);
        sb.append("', ");
        sb.append(minZoom);
        sb.append(", ");
        sb.append(maxZoom);
        sb.append(", ");
        sb.append(decimationFactor);
        return sb.toString();
    }

    /**
     * Convert string to dash.
     *
     * @param dashPattern the string to convert.
     * @return the dash array or null, if conversion failed.
     */
    public static float[] dashFromString( String dashPattern ) {
        if (dashPattern.trim().length() > 0) {
            String[] split = dashPattern.split(",");
            if (split.length > 1) {
                float[] dash = new float[split.length];
                for( int i = 0; i < split.length; i++ ) {
                    try {
                        float tmpDash = Float.parseFloat(split[i].trim());
                        dash[i] = tmpDash;
                    } catch (NumberFormatException e) {
                        // GPLog.error("Style", "Can't convert to dash pattern: " + dashPattern, e);
                        return null;
                    }
                }
                return dash;
            }
        }
        return null;
    }

    /**
     * Convert a dash array to string.
     *
     * @param dash  the dash to convert.
     * @param shift the shift.
     * @return the string representation.
     */
    public static String dashToString( float[] dash, Float shift ) {
        StringBuilder sb = new StringBuilder();
        if (shift != null)
            sb.append(shift);
        for( int i = 0; i < dash.length; i++ ) {
            if (shift != null || i > 0) {
                sb.append(",");
            }
            sb.append((int) dash[i]);
        }
        return sb.toString();
    }

    public static float[] getDashOnly( float[] shiftAndDash ) {
        return Arrays.copyOfRange(shiftAndDash, 1, shiftAndDash.length);
    }

    public static float getDashShift( float[] shiftAndDash ) {
        return shiftAndDash[0];
    }

    public String getTheme() throws Exception {
        if (themeMap != null && themeMap.size() > 0 && themeField != null && themeField.trim().length() > 0) {
            JSONObject root = new JSONObject();
            JSONObject unique = new JSONObject();
            root.put(UNIQUEVALUES, unique);
            JSONObject sub = new JSONObject();
            unique.put(themeField, sub);
            for( Entry<String, BasicStyle> entry : themeMap.entrySet() ) {
                String key = entry.getKey();
                BasicStyle value = entry.getValue();
                sub.put(key, value.toJson());
            }
            return root.toString();
        }
        return "";
    }

    private JSONObject toJson() throws Exception {
        JSONObject jobj = new JSONObject();
        jobj.put(ID, id);
        jobj.put(NAME, name);
        jobj.put(SIZE, size);
        if (fillcolor != null)
            jobj.put(FILLCOLOR, fillcolor);
        if (strokecolor != null)
            jobj.put(STROKECOLOR, strokecolor);
        jobj.put(FILLALPHA, fillalpha);
        jobj.put(STROKEALPHA, strokealpha);
        jobj.put(SHAPE, shape);
        jobj.put(WIDTH, width);
        jobj.put(LABELSIZE, labelsize);
        jobj.put(LABELFIELD, labelfield);
        jobj.put(LABELVISIBLE, labelvisible);
        jobj.put(ENABLED, enabled);
        jobj.put(ORDER, order);
        jobj.put(DASH, dashPattern);
        jobj.put(MINZOOM, minZoom);
        jobj.put(MAXZOOM, maxZoom);
        jobj.put(DECIMATION, decimationFactor);

        return jobj;
    }

    public void setFromJson( String json ) throws Exception {
        JSONObject jobj = new JSONObject(json);

        // note that getDouble is used to have android compatibility (do not use getFloat)
        id = jobj.getLong(ID);
        if (jobj.has(NAME))
            name = jobj.getString(NAME);
        if (jobj.has(SIZE))
            size = jobj.getDouble(SIZE);
        if (jobj.has(FILLCOLOR))
            fillcolor = jobj.getString(FILLCOLOR);
        if (jobj.has(STROKECOLOR))
            strokecolor = jobj.getString(STROKECOLOR);
        if (jobj.has(FILLALPHA))
            fillalpha = jobj.getDouble(FILLALPHA);
        if (jobj.has(STROKEALPHA))
            strokealpha = jobj.getDouble(STROKEALPHA);
        if (jobj.has(SHAPE))
            shape = jobj.getString(SHAPE);
        if (jobj.has(WIDTH))
            width = jobj.getDouble(WIDTH);
        if (jobj.has(LABELSIZE))
            labelsize = jobj.getDouble(LABELSIZE);
        if (jobj.has(LABELFIELD))
            labelfield = jobj.getString(LABELFIELD);
        if (jobj.has(LABELVISIBLE))
            labelvisible = jobj.getInt(LABELVISIBLE);
        if (jobj.has(ENABLED))
            enabled = jobj.getInt(ENABLED);
        if (jobj.has(ORDER))
            order = jobj.getInt(ORDER);
        if (jobj.has(DASH))
            dashPattern = jobj.getString(DASH);
        if (jobj.has(MINZOOM))
            minZoom = jobj.getInt(MINZOOM);
        if (jobj.has(MAXZOOM))
            maxZoom = jobj.getInt(MAXZOOM);
        if (jobj.has(DECIMATION))
            decimationFactor = jobj.getDouble(DECIMATION);

    }

    public String toString() {
        try {
            String jsonStr = getTheme();
            if (jsonStr.length() == 0) {
                return toJson().toString();
            }
            return jsonStr;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

}
