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
package org.hortonmachine.gears.io.las.index;

import org.locationtech.jts.geom.*;
import oms3.annotations.*;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.index.strtree.STRtreeJGT;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Description("Reads indexes of Las files.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("las, lidar")
@Label(HMConstants.LESTO)
@Name("lasindexreader")
@Status(5)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class OmsLasIndexReader extends HMModel {

    @Description("The las index file.")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFile;

    @Description("The bounds of data to extract.")
    @In
    public SimpleFeatureCollection inBounds;

    @Description("The optional code defining the target coordinate reference system. This is needed only if the file has no prj file. If set, it will be used over the prj file.")
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("Flag to create only the boundary polygon from the envelope.")
    @In
    public boolean doBounds;

    @Description("The extracted data or bounds.")
    @Out
    public SimpleFeatureCollection outData;

    /**
     * Can be used to avoid feature creation and instead use an 
     * internal format with less overhead.
     */
    public boolean doInternal = false;
    public List<LasRecord> lasPoints = new ArrayList<LasRecord>();

    @SuppressWarnings("rawtypes")
    @Execute
    public void process() throws Exception {
        checkNull(inFile);
        if (!doBounds) {
            checkNull(inBounds);
        }

        CoordinateReferenceSystem crs = null;
        try {
            crs = CrsUtilities.readProjectionFile(inFile, "lasfolder");
        } catch (Exception e) {
            // ignore and try from vars
        }
        if (crs == null && pCode == null) {
            throw new ModelsIllegalargumentException("Either a .prj file of an EPSG code needs to be supplied.", this);
        }
        if (crs == null)
            crs = CrsUtilities.getCrsFromEpsg(pCode, null);
        GeometryFactory gf = GeometryUtilities.gf();

        File parentFolder = new File(inFile).getParentFile();
        STRtreeJGT mainIndexTree = readIndex(inFile);

        List<Geometry> boundsList;
        if (!doBounds) {
            boundsList = FeatureUtilities.featureCollectionToGeometriesList(inBounds, true, null);
        } else {
            // include everything
            boundsList = new ArrayList<Geometry>();
            boundsList.add(gf.createPolygon(new Coordinate[]{//
                    new Coordinate(-Double.MAX_VALUE, -Double.MAX_VALUE),//
                            new Coordinate(-Double.MAX_VALUE, Double.MAX_VALUE),//
                            new Coordinate(Double.MAX_VALUE, Double.MAX_VALUE),//
                            new Coordinate(Double.MAX_VALUE, -Double.MAX_VALUE),//
                            new Coordinate(-Double.MAX_VALUE, -Double.MAX_VALUE)//
                    }));
        }

        final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        SimpleFeatureBuilder builder = null;
        if (!doBounds) {
            b.setName("lasdata");
            b.setCRS(crs);
            b.add("the_geom", Point.class);
            b.add("elev", Double.class);
            b.add("intensity", Double.class);
            b.add("classification", Integer.class);
            b.add("impulse", Double.class);
            b.add("numimpulse", Double.class);
            SimpleFeatureType featureType = b.buildFeatureType();
            builder = new SimpleFeatureBuilder(featureType);
        } else {
            b.setName("lasdatabounds");
            b.setCRS(crs);
            b.add("the_geom", Polygon.class);
            b.add("file", String.class);
            SimpleFeatureType featureType = b.buildFeatureType();
            builder = new SimpleFeatureBuilder(featureType);
        }
        outData = new DefaultFeatureCollection();

        for( Geometry boundGeom : boundsList ) {
            Envelope env = boundGeom.getEnvelopeInternal();
            List filesList = mainIndexTree.query(env);
            for( Object fileName : filesList ) {
                if (fileName instanceof String) {
                    pm.message("Processing: " + fileName);
                    String name = (String) fileName;
                    File lasFile = new File(parentFolder, name);
                    File lasIndexFile = FileUtilities.substituteExtention(lasFile, "lasfix");
                    if (!lasIndexFile.exists() || !lasFile.exists()) {
                        continue;
                    }
                    ALasReader reader = ALasReader.getReader(lasFile, crs);
                    reader.open();
                    try {
                        ILasHeader header = reader.getHeader();

                        if (!doBounds) {
                            // TODO check files
                            STRtreeJGT lasIndex = readIndex(lasIndexFile.getAbsolutePath());
                            List lasIndexStoreInfoList = lasIndex.query(env);
                            pm.beginTask("Read data...", lasIndexStoreInfoList.size());
                            for( Object obj : lasIndexStoreInfoList ) {
                                if (obj instanceof double[]) {
                                    double[] addresses = (double[]) obj;
                                    long from = (long) addresses[0];
                                    long to = (long) addresses[1];
                                    for( long pointNum = from; pointNum < to; pointNum++ ) {
                                        LasRecord lasDot = reader.getPointAt(pointNum);
                                        if (doInternal) {
                                            lasPoints.add(lasDot);
                                        } else {
                                            final double x = lasDot.x;
                                            final double y = lasDot.y;
                                            final double z = lasDot.z;
                                            final double intensity = lasDot.intensity;
                                            final int classification = lasDot.classification;
                                            final double impulse = lasDot.returnNumber;
                                            final double impulseNumber = lasDot.numberOfReturns;

                                            final Coordinate tmp = new Coordinate(x, y, z);
                                            final Point point = gf.createPoint(tmp);
                                            final Object[] values = new Object[]{point, z, intensity, classification, impulse,
                                                    impulseNumber};
                                            builder.addAll(values);
                                            final SimpleFeature feature = builder.buildFeature(null);
                                            ((DefaultFeatureCollection) outData).add(feature);
                                        }
                                    }
                                }
                                pm.worked(1);
                            }
                            pm.done();
                        } else {
                            ReferencedEnvelope3D dataEnvelope = header.getDataEnvelope();
                            Polygon polygon = envelopeToPolygon(dataEnvelope);
                            final Object[] values = new Object[]{polygon, name};
                            builder.addAll(values);
                            final SimpleFeature feature = builder.buildFeature(null);
                            ((DefaultFeatureCollection) outData).add(feature);
                        }
                    } finally {
                        reader.close();
                    }
                }
            }
        }
    }

    public static STRtreeJGT readIndex( String path ) throws Exception {
        File file = new File(path);
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            long length = raf.length();
            // System.out.println(length + "/" + (int) length);
            byte[] bytes = new byte[(int) length];
            int read = raf.read(bytes);
            if (read != length) {
                throw new IOException();
            }
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object readObject = in.readObject();
            STRtreeJGT indexObj = (STRtreeJGT) readObject;
            return indexObj;
        } finally {
            if (raf != null)
                raf.close();
        }
    }

    public static Polygon envelopeToPolygon( Envelope envelope ) {
        double w = envelope.getMinX();
        double e = envelope.getMaxX();
        double s = envelope.getMinY();
        double n = envelope.getMaxY();

        Coordinate[] coords = new Coordinate[5];
        coords[0] = new Coordinate(w, n);
        coords[1] = new Coordinate(e, n);
        coords[2] = new Coordinate(e, s);
        coords[3] = new Coordinate(w, s);
        coords[4] = new Coordinate(w, n);

        GeometryFactory gf = GeometryUtilities.gf();
        LinearRing linearRing = gf.createLinearRing(coords);
        Polygon polygon = gf.createPolygon(linearRing, null);
        return polygon;
    }

    @Finalize
    public void close() throws Exception {
    }


}
