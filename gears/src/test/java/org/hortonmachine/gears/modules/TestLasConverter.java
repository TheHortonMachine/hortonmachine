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
package org.hortonmachine.gears.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.geotools.api.feature.simple.SimpleFeature;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.core.v_1_0.LasReaderBuffered;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.modules.v.vectorconverter.OmsLasConverter;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
@SuppressWarnings("nls")
public class TestLasConverter extends HMTestCase {
    public void testLasConverter() throws Exception {

        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        File tmpFile = File.createTempFile("jgt-", ".las");

        List<LasRecord> list = new ArrayList<LasRecord>();
        LasRecord r1 = new LasRecord();
        r1.x = 724765.28;
        r1.y = 5207440.54;
        r1.z = 1168.81;
        r1.intensity = 65;
        r1.returnNumber = 1;
        r1.numberOfReturns = 2;
        r1.classification = 3;
        list.add(r1);
        LasRecord r2 = new LasRecord();
        r2.x = 724765.28;
        r2.y = 5207439.57;
        r2.z = 1169.3700000000001;
        r2.intensity = 180;
        r2.returnNumber = 1;
        r2.numberOfReturns = 1;
        r2.classification = 3;
        list.add(r2);

        ALasWriter w = ALasWriter.getWriter(tmpFile, crs);
        w.setBounds(r1.x, r1.x, r2.y, r1.y, r1.z, r2.z);
        w.open();
        for( LasRecord lasRecord : list ) {
            w.addPoint(lasRecord);
        }
        w.close();

        ALasReader r = new LasReaderBuffered(tmpFile, crs);
        r.open();
        r.getHeader();
        assertTrue(r.hasNextPoint());
        LasRecord lr1 = r.getNextPoint();
        assertEquals(r1.x, lr1.x, DELTA);
        assertEquals(r1.y, lr1.y, DELTA);
        assertEquals(r1.z, lr1.z, DELTA);
        assertEquals(r1.intensity, lr1.intensity, DELTA);
        assertEquals(r1.returnNumber, lr1.returnNumber, DELTA);
        assertEquals(r1.numberOfReturns, lr1.numberOfReturns, DELTA);
        assertEquals(r1.classification, lr1.classification, DELTA);
        assertEquals(r1.gpsTime, lr1.gpsTime, DELTA);
        assertTrue(r.hasNextPoint());
        LasRecord lr2 = r.getNextPoint();
        assertEquals(r2.x, lr2.x, DELTA);
        assertEquals(r2.y, lr2.y, DELTA);
        assertEquals(r2.z, lr2.z, DELTA);
        assertEquals(r2.intensity, lr2.intensity, DELTA);
        assertEquals(r2.returnNumber, lr2.returnNumber, DELTA);
        assertEquals(r2.numberOfReturns, lr2.numberOfReturns, DELTA);
        assertEquals(r2.classification, lr2.classification, DELTA);
        assertEquals(r2.gpsTime, lr2.gpsTime, DELTA);

        r.close();

        tmpFile.delete();

    }

    /**
     * Pulses of 1 to 3 returns on a line of 10 m: the filters are combined, last keeps the last
     * return of each pulse, the box excludes the points outside of it.
     */
    public void testFilters() throws Exception {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        File folder = File.createTempFile("hm_lasconverter_", "");
        folder.delete();
        folder.mkdirs();
        try {
            List<LasRecord> points = new ArrayList<LasRecord>();
            for( int i = 0; i < 10; i++ ) {
                int returns = i % 3 + 1;
                for( int ret = 1; ret <= returns; ret++ ) {
                    LasRecord point = new LasRecord();
                    point.x = 700000 + i + 0.5;
                    point.y = 5200000.5;
                    point.z = 100.0 + 10 * (returns - ret);
                    point.intensity = (short) (10 * i);
                    point.returnNumber = (short) ret;
                    point.numberOfReturns = (short) returns;
                    point.classification = (short) (ret == returns ? 2 : 5);
                    points.add(point);
                }
            }
            // 4 pulses of 1 return, 3 of 2 and 3 of 3: 19 points
            File las = new File(folder, "pulses.las");
            try (ALasWriter w = ALasWriter.getWriter(las, crs)) {
                w.setBounds(700000, 700010, 5200000, 5200001, 100, 120);
                w.open();
                for( LasRecord point : points ) {
                    w.addPoint(point);
                }
            }

            assertEquals(19, convert(las, folder, c -> {
            }));
            // a last return for each pulse
            assertEquals(10, convert(las, folder, c -> c.pImpulses = OmsLasConverter.LAST_RETURN));
            // the first of the 6 pulses with more returns, and the last of every pulse
            assertEquals(16, convert(las, folder, c -> c.pImpulses = "1, last"));
            assertEquals(10, convert(las, folder, c -> c.pClasses = "2"));
            // the points 10 and 20 m over the ground
            assertEquals(9, convert(las, folder, c -> c.pElevrange = "105,120"));
            // from 3 to 5 m: pulses 3, 4 and 5 with 1, 2 and 3 returns
            assertEquals(6, convert(las, folder, c -> {
                c.pWest = 700003.0;
                c.pEast = 700006.0;
                c.pSouth = 5200000.0;
                c.pNorth = 5200001.0;
            }));
            // all combined: the last returns of pulses 3, 4 and 5 over 0 intensity
            assertEquals(3, convert(las, folder, c -> {
                c.pImpulses = "last";
                c.pIntensityrange = "1,1000";
                c.pWest = 700003.0;
                c.pEast = 700006.0;
                c.pSouth = 5200000.0;
                c.pNorth = 5200001.0;
            }));

            // the vector in a geopackage, with the classification
            OmsLasConverter converter = new OmsLasConverter();
            converter.inFile = las.getAbsolutePath();
            converter.pClasses = "5";
            converter.outFile = new File(folder, "points.gpkg").getAbsolutePath() + "#points";
            converter.process();
            List<SimpleFeature> features = FeatureUtilities
                    .featureCollectionToList(OmsVectorReader.readVector(converter.outFile));
            assertEquals(9, features.size());
            for( SimpleFeature feature : features ) {
                assertEquals(5, ((Number) feature.getAttribute("classification")).intValue());
            }
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }

    private static long convert( File las, File folder, Consumer<OmsLasConverter> setup ) throws Exception {
        OmsLasConverter converter = new OmsLasConverter();
        converter.inFile = las.getAbsolutePath();
        setup.accept(converter);
        File out = new File(folder, "filtered.las");
        converter.outFile = out.getAbsolutePath();
        converter.process();
        long count;
        try (ALasReader reader = ALasReader.getReader(out, null)) {
            reader.open();
            count = reader.getHeader().getRecordsCount();
        }
        FileUtilities.deleteFileOrDir(out);
        return count;
    }
}
