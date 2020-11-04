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
package org.hortonmachine.gears.modules.v.rastercattofeatureattribute;

import static org.hortonmachine.gears.modules.v.rastercattofeatureattribute.OmsRasterCatToFeatureAttribute.*;
import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.RenderedImage;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.features.FeatureExtender;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OMSRASTERCATTOFEATUREATTRIBUTE_DESCRIPTION)
@Documentation(OMSRASTERCATTOFEATUREATTRIBUTE_DOCUMENTATION)
@Author(name = OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORNAMES, contact = OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORCONTACTS)
@Keywords(OMSRASTERCATTOFEATUREATTRIBUTE_KEYWORDS)
@Label(OMSRASTERCATTOFEATUREATTRIBUTE_LABEL)
@Name(OMSRASTERCATTOFEATUREATTRIBUTE_NAME)
@Status(OMSRASTERCATTOFEATUREATTRIBUTE_STATUS)
@License(OMSRASTERCATTOFEATUREATTRIBUTE_LICENSE)
public class OmsRasterCatToFeatureAttribute extends HMModel {

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_F_NEW_DESCRIPTION)
    @In
    public String fNew = "new";

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_P_POS_DESCRIPTION)
    @In
    public String pPos = MIDDLE;

    @Description(OMSRASTERCATTOFEATUREATTRIBUTE_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector = null;

    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_DESCRIPTION = "Module that extracts raster categories and adds them to a feature collection.";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_DOCUMENTATION = "OmsRasterCatToFeatureAttribute.html";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_KEYWORDS = "Raster, Vector";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_LABEL = VECTORPROCESSING;
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_NAME = "rat2featureattr";
    public static final int OMSRASTERCATTOFEATUREATTRIBUTE_STATUS = 40;
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_IN_RASTER_DESCRIPTION = "The raster on which to map the vector features.";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_IN_VECTOR_DESCRIPTION = "The vector to use for the geometric mapping.";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_F_NEW_DESCRIPTION = "The name for the new field to create (if existing, the field is populated).";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_P_POS_DESCRIPTION = "The position of the coordinate to take in the case of multi geometries.";
    public static final String OMSRASTERCATTOFEATUREATTRIBUTE_OUT_VECTOR_DESCRIPTION = "The extended vector.";

    private static final String MIDDLE = "middle";
    private static final String START = "start";
    private static final String END = "end";

    private RandomIter inIter = null;

    private GridGeometry2D gridGeometry;

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        RenderedImage inputRI = inRaster.getRenderedImage();
        try {
            inIter = RandomIterFactory.create(inputRI, null);
            gridGeometry = inRaster.getGridGeometry();

            SimpleFeatureType featureType = inVector.getSchema();
            crs = inVector.getSchema().getCoordinateReferenceSystem();

            FeatureExtender fExt = null;

            Envelope2D inCoverageEnvelope = inRaster.getEnvelope2D();
            outVector = new DefaultFeatureCollection();
            FeatureIterator<SimpleFeature> featureIterator = inVector.features();
            int all = inVector.size();
            pm.beginTask("Extracting raster information...", all);
            String setFieldName = FeatureUtilities.findAttributeName(featureType, fNew);
            while( featureIterator.hasNext() ) {
                SimpleFeature feature = featureIterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                double value = -1;
                Coordinate c;
                Coordinate[] coordinates = geometry.getCoordinates();
                if (EGeometryType.isPoint(geometry)) {
                    c = coordinates[0];
                    if (!inCoverageEnvelope.contains(c.x, c.y)) {
                        continue;
                    }
                    value = getRasterValue(c);

                    if (fExt == null)
                        fExt = new FeatureExtender(featureType, //
                                new String[]{fNew}, //
                                new Class< ? >[]{Double.class});

                    SimpleFeature finalFeature;
                    if (setFieldName == null) {
                        finalFeature = fExt.extendFeature(feature, new Object[]{value});
                    } else {
                        feature.setAttribute(setFieldName, value);
                        finalFeature = feature;
                    }
                    ((DefaultFeatureCollection) outVector).add(finalFeature);
                } else if (EGeometryType.isLine(geometry)) {
                    if (pPos.trim().equalsIgnoreCase(START)) {
                        c = coordinates[0];
                    } else if (pPos.trim().equalsIgnoreCase(END)) {
                        c = coordinates[coordinates.length - 1];
                    } else {// (pPos.trim().equalsIgnoreCase(MIDDLE)) {
                        c = coordinates[coordinates.length / 2];
                    }
                    if (!inCoverageEnvelope.contains(c.x, c.y)) {
                        continue;
                    }
                    value = getRasterValue(c);
                    if (fExt == null)
                        fExt = new FeatureExtender(featureType, //
                                new String[]{fNew}, //
                                new Class< ? >[]{Double.class});
                    SimpleFeature finalFeature;
                    if (setFieldName == null) {
                        finalFeature = fExt.extendFeature(feature, new Object[]{value});
                    } else {
                        feature.setAttribute(setFieldName, value);
                        finalFeature = feature;
                    }
                    ((DefaultFeatureCollection) outVector).add(finalFeature);
                } else if (EGeometryType.isPolygon(geometry)) {
                    if (fExt == null) {
                        String max = fNew + "_max";
                        String min = fNew + "_min";
                        String avg = fNew + "_avg";
                        String sum = fNew + "_sum";
                        fExt = new FeatureExtender(featureType, //
                                new String[]{min, max, avg, sum}, //
                                new Class< ? >[]{Double.class, Double.class, Double.class, Double.class});
                    }

                    SimpleFeature singleFeature = FeatureUtilities.toDummyFeature(geometry, crs);
                    SimpleFeatureCollection newCollection = new DefaultFeatureCollection();
                    ((DefaultFeatureCollection) newCollection).add(singleFeature);
                    OmsScanLineRasterizer raster = new OmsScanLineRasterizer();
                    raster.inVector = newCollection;
                    raster.inRaster = inRaster;
                    raster.pValue = 1.0;
                    raster.process();
                    GridCoverage2D rasterizedVector = raster.outRaster;

                    double[] minMaxAvgSum = OmsRasterSummary.getMinMaxAvgSum(rasterizedVector);
                    SimpleFeature extendedFeature = fExt.extendFeature(feature,
                            new Object[]{minMaxAvgSum[0], minMaxAvgSum[1], minMaxAvgSum[2], minMaxAvgSum[3]});
                    ((DefaultFeatureCollection) outVector).add(extendedFeature);
                } else {
                    throw new ModelsIllegalargumentException("The Geometry type is not supported.", this, pm);
                }

                pm.worked(1);
            }
            featureIterator.close();
            pm.done();
        } finally {
            inIter.done();
        }

    }

    private double getRasterValue( Coordinate c ) throws TransformException {
        double value;
        GridCoordinates2D gridCoord = gridGeometry.worldToGrid(new DirectPosition2D(c.x, c.y));
        value = inIter.getSampleDouble(gridCoord.x, gridCoord.y, 0);

        // TODO make this better
        if (isNovalue(value) || value >= Float.MAX_VALUE || value <= -Float.MAX_VALUE) {
            value = -9999.0;
        }
        return value;
    }
}
