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
package org.hortonmachine.lesto.modules.utilities;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;

@Description("Creates a vector map of the point cloud density over a given grid.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("las, density, vector")
@Label(HMConstants.LESTO + "/utilities")
@Name("laspointdensityextractor")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class LasPointDensityExtractor extends HMModel {

    @Description("Las file path.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inFile;

    @Description("The grid resolution.")
    @Unit("m")
    @In
    public double pGridStep = 1.0;

    @Description("Output vector map.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFile;

    private CoordinateReferenceSystem crs;

    @Execute
    public void process() throws Exception {
        checkNull(inFile);

        if (pGridStep <= 0) {
            throw new ModelsIllegalargumentException("The grid step has to be major than 0.", this);
        }

        List<DensityData> densityDataList = new ArrayList<DensityData>();
        final File lasFile = new File(inFile);
        try (ALasReader lasReader = ALasReader.getReader(lasFile, null)) {
            lasReader.open();

            ILasHeader header = lasReader.getHeader();
            crs = header.getCrs();
            ReferencedEnvelope3D dataEnvelope = header.getDataEnvelope();

            double[] xBins = NumericsUtilities.range2Bins(dataEnvelope.getMinX(), dataEnvelope.getMaxX(), pGridStep, false);
            double[] yBins = NumericsUtilities.range2Bins(dataEnvelope.getMinY(), dataEnvelope.getMaxY(), pGridStep, false);

            STRtree tree = new STRtree();
            for( int x = 0; x < xBins.length - 1; x++ ) {
                double minX = xBins[x];
                double maxX = xBins[x + 1];
                for( int y = 0; y < yBins.length - 1; y++ ) {
                    double minY = yBins[y];
                    double maxY = yBins[y + 1];
                    Envelope envelope = new Envelope(minX, maxX, minY, maxY);
                    DensityData densityData = new DensityData();
                    tree.insert(envelope, densityData);
                    densityDataList.add(densityData);
                    Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(envelope);
                    densityData.geometry = polygon;
                }
            }

            final long recordsCount = header.getRecordsCount();
            pm.beginTask("Sorting las data...", (int) recordsCount);
            while( lasReader.hasNextPoint() ) {
                pm.worked(1);

                final LasRecord lasDot = lasReader.getNextPoint();

                final double x = lasDot.x;
                final double y = lasDot.y;
                final short impulse = lasDot.returnNumber;

                Coordinate p = new Coordinate(x, y);
                Envelope envelope = new Envelope(p);
                List result = tree.query(envelope);
                if (result.size() < 1)
                    throw new RuntimeException();

                DensityData densityData = null;
                if (result.size() == 1) {
                    densityData = (DensityData) result.get(0);
                } else {
                    for( Object object : result ) {
                        densityData = (DensityData) object;
                        Point point = gf.createPoint(p);
                        if (densityData.geometry.intersects(point)) {
                            break;
                        }
                    }
                }
                densityData.imp[impulse]++; // update impulse count
                densityData.imp[0]++; // update total count
            }
            pm.done();

        }

        DefaultFeatureCollection outGeodata = new DefaultFeatureCollection();

        final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("lasdata");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("imp1", Integer.class);
        b.add("imp2", Integer.class);
        b.add("imp3", Integer.class);
        b.add("imp4", Integer.class);
        b.add("imp5", Integer.class);
        b.add("total", Integer.class);
        final SimpleFeatureType type = b.buildFeatureType();
        final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        for( DensityData d : densityDataList ) {
            Object[] objs = {d.geometry, d.imp[1], d.imp[2], d.imp[3], d.imp[4], d.imp[5], d.imp[0]};

            if (d.imp[0] > 0) {
                builder.addAll(objs);
                final SimpleFeature feature = builder.buildFeature(null);

                outGeodata.add(feature);
            }
        }

        dumpVector(outGeodata, outFile);
    }

    private static class DensityData {
        /**
         * contains the count of [total, imp1, imp2, imp3, imp4, imp5]
         */
        int[] imp = new int[6];
        Geometry geometry;
    }

}
