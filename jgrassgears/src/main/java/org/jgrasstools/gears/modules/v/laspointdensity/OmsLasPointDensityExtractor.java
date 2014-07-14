/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules.v.laspointdensity;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.strtree.STRtree;
import oms3.annotations.*;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.jgrasstools.gears.i18n.GearsMessages.*;

@Description("Creates a vector map of the point cloud density over a given grid.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("las, density, vector")
@Label(JGTConstants.VECTORPROCESSING)
@Name("pointdensityextractor")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsLasPointDensityExtractor extends JGTModel {

    @Description(OMSLASCONVERTER_inFile_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile;

    @Description(OMSLASCONVERTER_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The grid resolution.")
    @In
    public double pGridStep = 1.0;


    private GeometryFactory gf = GeometryUtilities.gf();

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        checkNull(inFile);

        if (pGridStep <= 0) {
            throw new ModelsIllegalargumentException("The grid step has to be major than 0.", this);
        }

        CoordinateReferenceSystem crs = null;
        if (pCode != null) {
            crs = CRS.decode(pCode);
        } else {
            // read the prj file
            crs = CrsUtilities.readProjectionFile(inFile, "las");
        }

        List<DensityData> densityDataList = new ArrayList<DensityData>();
        final File lasFile = new File(inFile);
        ALasReader lasReader = ALasReader.getReader(lasFile, crs);
        try {
            lasReader.open();

            ILasHeader header = lasReader.getHeader();
            ReferencedEnvelope3D dataEnvelope = header.getDataEnvelope();

            double[] xBins = NumericsUtilities.range2Bins(dataEnvelope.getMinX(), dataEnvelope.getMaxX(), pGridStep, false);
            double[] yBins = NumericsUtilities.range2Bins(dataEnvelope.getMinY(), dataEnvelope.getMaxY(), pGridStep, false);

            STRtree tree = new STRtree();
            for (int x = 0; x < xBins.length - 1; x++) {
                double minX = xBins[x];
                double maxX = xBins[x + 1];
                for (int y = 0; y < yBins.length - 1; y++) {
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
            // long index = 0;
            long addedFeatures = 0;

            pm.beginTask("Sorting las data...", (int) recordsCount);
            while (lasReader.hasNextPoint()) {
                pm.worked(1);

                final LasRecord lasDot = lasReader.getNextPoint();

                final double x = lasDot.x;
                final double y = lasDot.y;
                final short impulse = lasDot.returnNumber;
                final short impulseNumber = lasDot.numberOfReturns;

                Envelope envelope = new Envelope(new Coordinate(x, y));
                List result = tree.query(envelope);
                if (result.size() != 1)
                    throw new RuntimeException();

                DensityData densityData = (DensityData) result.get(0);

                densityData.imp[impulse]++; // update impulse count
                densityData.imp[0]++; // update total count
            }
            pm.done();

        } finally {
            if (lasReader != null)
                lasReader.close();
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

        for (DensityData d : densityDataList) {
            Object[] objs = {d.geometry, d.imp[1], d.imp[2], d.imp[3], d.imp[4], d.imp[5], d.imp[0]};

            builder.addAll(objs);
            final SimpleFeature feature = builder.buildFeature(null);

            outGeodata.add(feature);
        }

        File inputFile = new File(inFile);
        String name = FileUtilities.getNameWithoutExtention(inputFile);
        File parentFile = inputFile.getParentFile();
        File outFile = new File(parentFile, name + "_density.shp");
        dumpVector(outGeodata, outFile.getAbsolutePath());
    }

    private static class DensityData {
        /**
         * contains the count of [total, imp1, imp2, imp3, imp4, imp5]
         */
        int[] imp = new int[5];
        Geometry geometry;
    }
}
