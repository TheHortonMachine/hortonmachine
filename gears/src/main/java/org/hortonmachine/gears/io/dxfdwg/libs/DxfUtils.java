/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Michael Michaud
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
package org.hortonmachine.gears.io.dxfdwg.libs;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hortonmachine.gears.io.dxfdwg.libs.dxf.DxfGroup;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Utils for DXF format. 
 * 
 * <p>Based on work of: Michael Michaud</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DxfUtils {
    public static final String SUFFIX = "_";
    public static final String ZERO = "0.0";
    public static final String SEQEND = "SEQEND";
    public static final String VERTEX = "VERTEX";
    public static final String LINE = "LINE";
    public static final String POLYLINE = "POLYLINE";
    public static final String POINT = "POINT";
    public static final String LAYER = "LAYER";
    public static final String TEXT_STYLE = "TEXT_STYLE";
    public static final String BYLAYER = "BYLAYER";
    public static final String LTYPE = "LTYPE";
    public static final String ELEVATION = "ELEVATION";
    public static final String THICKNESS = "THICKNESS";
    public static final String COLOR = "COLOR";
    public static final String TEXT_HEIGHT = "TEXT_HEIGHT";
    public static final String TEXT = "TEXT";

    public static final int precision = 3;

    private DxfUtils() {}

    /**
     * Write a {@link SimpleFeature} to dxf string.
     * 
     * @param featureMate the feature to convert.
     * @param layerName the layer name in case none is in the attributes.
     * @param elevationAttrName the attribute defining elevation or <code>null</code>.
     * @param suffix <code>true</code> if suffix is needed.
     * @param force2CoordsToLine if <code>true</code>, lines that are composed of just 2 coordinates
     *                      will be handled as LINE instead of the default which is POLYLINE.
     * @return the string representation.
     */
    public static String feature2Dxf( FeatureMate featureMate, String layerName, String elevationAttrName, boolean suffix,
            boolean force2CoordsToLine ) {
        Geometry g = featureMate.getGeometry();

        if (EGeometryType.isPoint(g)) {
            return point2Dxf(featureMate, layerName, elevationAttrName);
        } else if (EGeometryType.isLine(g)) {
            return lineString2Dxf(featureMate, layerName, elevationAttrName, force2CoordsToLine);
        } else if (EGeometryType.isPolygon(g)) {
            return polygon2Dxf(featureMate, layerName, elevationAttrName, suffix);
        } else if (g instanceof GeometryCollection) {
            StringBuilder sb = new StringBuilder();
            for( int i = 0; i < g.getNumGeometries(); i++ ) {
                SimpleFeature ff = SimpleFeatureBuilder.copy(featureMate.getFeature());
                ff.setDefaultGeometry(g.getGeometryN(i));
                FeatureMate fm = new FeatureMate(ff);
                sb.append(feature2Dxf(fm, layerName, elevationAttrName, suffix, force2CoordsToLine));
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    private static String point2Dxf( FeatureMate featureMate, String layerName, String elevationAttrName ) {

        StringBuilder sb;

        // TEXT
        SimpleFeature feature = featureMate.getFeature();
        Object attribute = FeatureUtilities.getAttributeCaseChecked(feature, TEXT);
        boolean hasText = attribute != null && !attribute.equals("");
        if (hasText) {
            sb = new StringBuilder(DxfGroup.toString(0, TEXT));
        } else {
            sb = new StringBuilder(DxfGroup.toString(0, POINT));
        }

        // LAYER
        handleLAYER(feature, layerName, sb);

        // LTYPE
        handleLTYPE(feature, sb);

        // ELEVATION
        handleELEVATION(feature, sb);

        // THICKNESS
        handleTHICKNESS(feature, sb);

        // COLOR
        handleColor(feature, sb);

        Coordinate coord = ((Point) featureMate.getGeometry()).getCoordinate();
        if (elevationAttrName != null) {
            Double elev = featureMate.getAttribute(elevationAttrName, Double.class);
            if (elev != null) {
                coord.z = elev;
            }
        }

        sb.append(DxfGroup.toString(10, coord.x, precision));
        sb.append(DxfGroup.toString(20, coord.y, precision));
        if (!Double.isNaN(coord.z))
            sb.append(DxfGroup.toString(30, coord.z, precision));

        handleTEXTHEIGHT(feature, sb, hasText);
        return sb.toString();
    }

    private static String lineString2Dxf( FeatureMate featureMate, String layerName, String elevationAttrName,
            boolean force2CoordsToLine ) {
        Geometry geom = featureMate.getGeometry();
        Coordinate[] coords = geom.getCoordinates();
        // Correction added by L. Becker and R Littlefield on 2006-11-08
        // It writes 2 points-only polylines in a line instead of a polyline
        // to make it possible to incorporate big dataset in View32
        boolean is2CoordsLine = coords.length == 2;
        boolean doLine = is2CoordsLine && force2CoordsToLine;
        StringBuilder sb;
        if (doLine) {
            sb = new StringBuilder(DxfGroup.toString(0, LINE));
        } else {
            sb = new StringBuilder(DxfGroup.toString(0, POLYLINE));
        }

        double elev = Double.NaN;
        if (elevationAttrName != null) {
            Double tmp = featureMate.getAttribute(elevationAttrName, Double.class);
            if (tmp != null) {
                elev = tmp;
            }
        }

        SimpleFeature feature = featureMate.getFeature();
        handleLAYER(feature, layerName, sb);
        handleLTYPE(feature, sb);
        handleELEVATION(feature, sb);
        handleTHICKNESS(feature, sb);
        handleColor(feature, sb);

        // modified by L. Becker and R. Littlefield (add the Line case)
        if (doLine) {
            sb.append(DxfGroup.toString(10, coords[0].x, precision));
            sb.append(DxfGroup.toString(20, coords[0].y, precision));
            coords[0].z = elev;
            if (!Double.isNaN(coords[0].z))
                sb.append(DxfGroup.toString(30, ZERO));
            sb.append(DxfGroup.toString(11, coords[1].x, precision));
            sb.append(DxfGroup.toString(21, coords[1].y, precision));
            coords[1].z = elev;
            if (!Double.isNaN(coords[1].z))
                sb.append(DxfGroup.toString(31, ZERO));
        } else {
            sb.append(DxfGroup.toString(66, 1));
            sb.append(DxfGroup.toString(10, ZERO));
            sb.append(DxfGroup.toString(20, ZERO));
            coords[0].z = elev;
            if (!Double.isNaN(coords[0].z))
                sb.append(DxfGroup.toString(30, ZERO));
            sb.append(DxfGroup.toString(70, 8));

            for( int i = 0; i < coords.length; i++ ) {
                sb.append(DxfGroup.toString(0, VERTEX));
                handleLAYER(feature, layerName, sb);
                sb.append(DxfGroup.toString(10, coords[i].x, precision));
                sb.append(DxfGroup.toString(20, coords[i].y, precision));
                coords[i].z = elev;
                if (!Double.isNaN(coords[i].z))
                    sb.append(DxfGroup.toString(30, coords[i].z, precision));
                sb.append(DxfGroup.toString(70, 32));
            }
            sb.append(DxfGroup.toString(0, SEQEND));
        }
        return sb.toString();
    }

    private static String polygon2Dxf( FeatureMate featureMate, String layerName, String elevationAttrName, boolean suffix ) {
        Geometry geometry = featureMate.getGeometry();
        int numGeometries = geometry.getNumGeometries();
        StringBuilder sb = new StringBuilder();
        for( int g = 0; g < numGeometries; g++ ) {
            Polygon geom = (Polygon) geometry.getGeometryN(g);

            Coordinate[] coords = geom.getExteriorRing().getCoordinates();
            sb.append(DxfGroup.toString(0, POLYLINE));
            sb.append(DxfGroup.toString(8, layerName));

            SimpleFeature feature = featureMate.getFeature();
            handleLTYPE(feature, sb);
            handleELEVATION(feature, sb);
            handleTHICKNESS(feature, sb);
            handleColor(feature, sb);

            double elev = Double.NaN;
            if (elevationAttrName != null) {
                Double tmp = featureMate.getAttribute(elevationAttrName, Double.class);
                if (tmp != null) {
                    elev = tmp;
                }
            }

            sb.append(DxfGroup.toString(66, 1));
            sb.append(DxfGroup.toString(10, ZERO));
            sb.append(DxfGroup.toString(20, ZERO));
            coords[0].z = elev;
            if (!Double.isNaN(coords[0].z))
                sb.append(DxfGroup.toString(30, ZERO));
            sb.append(DxfGroup.toString(70, 9));
            for( int i = 0; i < coords.length; i++ ) {
                sb.append(DxfGroup.toString(0, VERTEX));
                sb.append(DxfGroup.toString(8, layerName));
                sb.append(DxfGroup.toString(10, coords[i].x, precision));
                sb.append(DxfGroup.toString(20, coords[i].y, precision));
                coords[i].z = elev;
                if (!Double.isNaN(coords[i].z))
                    sb.append(DxfGroup.toString(30, coords[i].z, precision));
                sb.append(DxfGroup.toString(70, 32));
            }
            sb.append(DxfGroup.toString(0, SEQEND));
            for( int h = 0; h < geom.getNumInteriorRing(); h++ ) {
                sb.append(DxfGroup.toString(0, POLYLINE));
                if (suffix)
                    sb.append(DxfGroup.toString(8, layerName + SUFFIX));
                else
                    sb.append(DxfGroup.toString(8, layerName));

                handleLTYPE(feature, sb);
                handleTHICKNESS(feature, sb);
                handleColor(feature, sb);

                sb.append(DxfGroup.toString(66, 1));
                sb.append(DxfGroup.toString(10, ZERO));
                sb.append(DxfGroup.toString(20, ZERO));
                coords[0].z = elev;
                if (!Double.isNaN(coords[0].z))
                    sb.append(DxfGroup.toString(30, ZERO));
                sb.append(DxfGroup.toString(70, 9));
                coords = geom.getInteriorRingN(h).getCoordinates();
                for( int i = 0; i < coords.length; i++ ) {
                    sb.append(DxfGroup.toString(0, VERTEX));
                    if (suffix)
                        sb.append(DxfGroup.toString(8, layerName + SUFFIX));
                    else
                        sb.append(DxfGroup.toString(8, layerName));
                    sb.append(DxfGroup.toString(10, coords[i].x, precision));
                    sb.append(DxfGroup.toString(20, coords[i].y, precision));
                    coords[i].z = elev;
                    if (!Double.isNaN(coords[i].z))
                        sb.append(DxfGroup.toString(30, coords[i].z, precision));
                    sb.append(DxfGroup.toString(70, 32));
                }
                sb.append(DxfGroup.toString(0, SEQEND));
            }
        }
        return sb.toString();
    }

    private static void handleLAYER( SimpleFeature feature, String layerName, StringBuilder sb ) {
        Object attribute;
        attribute = FeatureUtilities.getAttributeCaseChecked(feature, LAYER);
        if (attribute != null && !attribute.equals("")) {
            sb.append(DxfGroup.toString(8, feature.getAttribute(LAYER)));
        } else {
            sb.append(DxfGroup.toString(8, layerName));
        }
    }

    private static void handleTEXTHEIGHT( SimpleFeature feature, StringBuilder sb, boolean hasText ) {
        Object attribute;
        attribute = FeatureUtilities.getAttributeCaseChecked(feature, TEXT_HEIGHT);
        boolean hasTextHeight = attribute != null && !attribute.equals(new Float(0f));
        if (hasTextHeight) {
            sb.append(DxfGroup.toString(40, feature.getAttribute(TEXT_HEIGHT)));
        }
        if (hasText && hasTextHeight) {
            sb.append(DxfGroup.toString(1, feature.getAttribute(TEXT)));
            sb.append(DxfGroup.toString(7, feature.getAttribute(TEXT_STYLE)));
        }
    }

    private static void handleColor( SimpleFeature feature, StringBuilder sb ) {
        Object attribute = FeatureUtilities.getAttributeCaseChecked(feature, COLOR);
        if (attribute != null && ((Integer) attribute).intValue() != 256) {
            sb.append(DxfGroup.toString(62, feature.getAttribute(COLOR).toString()));
        }
    }

    private static void handleTHICKNESS( SimpleFeature feature, StringBuilder sb ) {
        Object attribute = FeatureUtilities.getAttributeCaseChecked(feature, THICKNESS);
        if (attribute != null && !attribute.equals(new Float(0f))) {
            sb.append(DxfGroup.toString(39, feature.getAttribute(THICKNESS)));
        }else{
            sb.append(DxfGroup.toString(39, 100));
        }
    }

    private static void handleELEVATION( SimpleFeature feature, StringBuilder sb ) {
        Object attribute = FeatureUtilities.getAttributeCaseChecked(feature, ELEVATION);
        if (attribute != null && !attribute.equals(new Float(0f))) {
            sb.append(DxfGroup.toString(38, feature.getAttribute(ELEVATION)));
        }
    }

    private static void handleLTYPE( SimpleFeature feature, StringBuilder sb ) {
        Object attribute = FeatureUtilities.getAttributeCaseChecked(feature, LTYPE);
        if (attribute != null && !attribute.equals(BYLAYER)) {
            sb.append(DxfGroup.toString(6, feature.getAttribute(LTYPE)));
        }
    }

}
