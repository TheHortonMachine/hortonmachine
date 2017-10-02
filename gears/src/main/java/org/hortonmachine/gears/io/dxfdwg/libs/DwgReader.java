/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.io.dxfdwg.libs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.DwgFile;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.DwgObject;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgArc;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgAttrib;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgCircle;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgLine;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgLwPolyline;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgMText;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgPoint;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgPolyline2D;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgPolyline3D;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgSolid;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgText;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Feature reader for DWG files.
 * 
 * <p>
 * We are going to implement the required methods, based on the DWG classes
 * available from jdwglibs
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.0.0
 */
public class DwgReader {
    DwgFile file;
    FeatureIterator<SimpleFeature> enumeration;
    int chosenLayerType = -1;

    private static final String LINES = "lines";

    // different feature types
    private DefaultFeatureCollection contourFeatures = new DefaultFeatureCollection();
    private DefaultFeatureCollection multiLineFeatures = new DefaultFeatureCollection();
    private DefaultFeatureCollection multiPointFeatures = new DefaultFeatureCollection();
    private DefaultFeatureCollection multiPolygonFeatures = new DefaultFeatureCollection();
    private DefaultFeatureCollection textFeatures = new DefaultFeatureCollection();
    private DefaultFeatureCollection attributesFeatures = new DefaultFeatureCollection();

    /**
     * A basic reader based on a DwgFile.
     * 
     * @param layerName
     *            Should match a layerName in the Dwg file
     * @param outFile
     * @param firstRound
     */
    public DwgReader( DwgFile pFile, GeometryTranslator gTranslator ) throws SchemaException {
        this.file = pFile;

        Vector dwgObjects = file.getDwgObjects();

        int cat = 0;
        int elenmentnum = dwgObjects.size();
        for( int i = 0; i < elenmentnum; i++ ) {
            DwgObject entity = (DwgObject) dwgObjects.get(i);

            String layerName = pFile.getLayerName(entity);
            if (entity instanceof DwgArc) {
                DwgArc arc = (DwgArc) entity;
                SimpleFeature feature = gTranslator.convertDwgArc(LINES, layerName, arc, cat);
                multiLineFeatures.add(feature);
            } else if (entity instanceof DwgCircle) {
                DwgCircle circle = (DwgCircle) entity;
                SimpleFeature feature = gTranslator.convertDwgCircle("polygons", layerName, circle, cat);
                multiPolygonFeatures.add(feature);
            } else if (entity instanceof DwgLine) {
                DwgLine line = (DwgLine) entity;
                SimpleFeature feature = gTranslator.convertDwgLine(LINES, layerName, line, cat);
                multiLineFeatures.add(feature);
            } else if (entity instanceof DwgPoint) {
                DwgPoint point = (DwgPoint) entity;
                SimpleFeature feature = gTranslator.convertDwgPoint("points", layerName, point, cat);
                multiPointFeatures.add(feature);
            } else if (entity instanceof DwgPolyline2D) {
                DwgPolyline2D polyline2d = (DwgPolyline2D) entity;
                SimpleFeature feature = gTranslator.convertDwgPolyline2D(LINES, layerName, polyline2d, cat);
                if (feature != null)
                    multiLineFeatures.add(feature);
            } else if (entity instanceof DwgPolyline3D) {
                DwgPolyline3D polyline3d = (DwgPolyline3D) entity;
                SimpleFeature feature = gTranslator.convertDwgPolyline3D(LINES, layerName, polyline3d, cat);
                if (feature != null)
                    multiLineFeatures.add(feature);
            } else if (entity instanceof DwgText) {
                DwgText text = (DwgText) entity;
                SimpleFeature feature = gTranslator.convertDwgText("text", layerName, text, cat);
                textFeatures.add(feature);
            } else if (entity instanceof DwgAttrib) {
                DwgAttrib attribute = (DwgAttrib) entity;
                SimpleFeature feature = gTranslator.convertDwgAttribute("text", layerName, attribute, cat);
                attributesFeatures.add(feature);
            } else if (entity instanceof DwgMText) {
                DwgMText text = (DwgMText) entity;
                SimpleFeature feature = gTranslator.convertDwgMText("text", layerName, text, cat);
                textFeatures.add(feature);
            } else if (entity instanceof DwgSolid) {
                DwgSolid solid = (DwgSolid) entity;
                SimpleFeature feature = gTranslator.convertDwgSolid("polygon", layerName, solid, cat);
                multiPolygonFeatures.add(feature);
            } else if (entity instanceof DwgLwPolyline) {
                DwgLwPolyline lwPolyline = (DwgLwPolyline) entity;
                SimpleFeature feature = gTranslator.convertDwgLwPolyline(LINES, layerName, lwPolyline, cat);
                multiLineFeatures.add(feature);
            }
            cat++;

        }
    }

    public Map<String, SimpleFeatureCollection> getFeatureCollectionsMap() throws IOException {
        Map<String, SimpleFeatureCollection> map = new HashMap<>();
        if (!textFeatures.isEmpty()) {
            map.put("text", textFeatures);
        }
        if (!attributesFeatures.isEmpty()) {
            map.put("text", attributesFeatures);
        }
        if (!multiLineFeatures.isEmpty()) {
            map.put(LINES, multiLineFeatures);
        }
        if (!contourFeatures.isEmpty()) {
            map.put(LINES, contourFeatures);
        }
        if (!multiPointFeatures.isEmpty()) {
            map.put("points", multiPointFeatures);
        }
        if (!multiPolygonFeatures.isEmpty()) {
            map.put("polygons", multiPolygonFeatures);
        }
        return map;
    }

    public SimpleFeatureCollection getTextFeatures() {
        return textFeatures;
    }

    public SimpleFeatureCollection getAttributesFeatures() {
        return attributesFeatures;
    }
    public SimpleFeatureCollection getMultiLineFeatures() {
        return multiLineFeatures;
    }

    public SimpleFeatureCollection getContourFeatures() {
        return contourFeatures;
    }

    public SimpleFeatureCollection getMultiPointFeatures() {
        return multiPointFeatures;
    }

    public SimpleFeatureCollection getMultiPolygonFeatures() {
        return multiPolygonFeatures;
    }

    public synchronized void close() throws IOException {
        if (file != null) {
            file = null;
        }
        enumeration = null;
    }

}
