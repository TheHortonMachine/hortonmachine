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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.hortonmachine.gears.utils.HMTestCase;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.io.WKTReader;

/**
 * Test {@link Quadtree} serialization.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestJtsQuadtreeSerialization extends HMTestCase {

    public void testJtsQuadtreeSerialization() throws Exception {

        WKTReader r = new WKTReader();
        Geometry polygon = r.read("POLYGON ((210 350, 230 310, 290 350, 290 350, 210 350))");
        polygon.setUserData(new Integer(1));
        Geometry line = r.read("LINESTRING (50 380, 90 210, 180 160, 240 40, 240 40)");
        line.setUserData(new Integer(2));
        Geometry point = r.read("POINT (130 120)");
        point.setUserData(new Integer(3));

        Geometry[] geoms = {point, line, polygon};

        // put geometries serialized in quadtree
        Quadtree tree = new Quadtree();
        for( int i = 0; i < geoms.length; i++ ) {
            Geometry geometry = geoms[i];
            byte[] geomBytes = serialize(geometry);

            Envelope envelope = geometry.getEnvelopeInternal();
            tree.insert(envelope, geomBytes);
        }

        // serialize tree
        byte[] serializedTree = serialize(tree);

        // deserialize tree
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedTree));
        Quadtree deserializedTree = (Quadtree) in.readObject();

        // query the polygon envelope and extract the geometries
        Envelope polEnvelope = polygon.getEnvelopeInternal();
        List geomList = deserializedTree.query(polEnvelope);
        assertTrue(geomList.size() == 2);
        byte[] polygonGeomObj = (byte[]) geomList.get(0);
        in = new ObjectInputStream(new ByteArrayInputStream(polygonGeomObj));
        Geometry geometry = (Geometry) in.readObject();
        Integer userData = (Integer) geometry.getUserData();
        if (userData == 2) {
            assertEquals("LineString", geometry.getGeometryType());
        }
        if (userData == 1) {
            assertEquals("Polygon", geometry.getGeometryType());
        }

    }

    private static byte[] serialize( Object obj ) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(obj);
        out.close();
        byte[] treeBytes = bos.toByteArray();
        return treeBytes;
    }
}
