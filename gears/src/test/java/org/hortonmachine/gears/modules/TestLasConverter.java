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

import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.core.v_1_0.LasReaderBuffered;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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
}
