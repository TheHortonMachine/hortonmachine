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
package org.hortonmachine.gears.io.disktree;


/**
 * Interface for DiskTree I/O modules.
 * 
 * <p>
 * The disktree binary file is defined as follows:
 * 
 * <ul>
 *  <li>6 bytes containing the chars 'jts'</li>
 *  <li>4 bytes for the jts major version</li>
 *  <li>4 bytes for the jts minor version</li>
 *  <li>8 bytes containing the address at which the quadtree is stored</li>
 *  <li>8 bytes containing the size of the stored quadtree</li>
 *  <li>then the WKB-ized geometries are stored</li>
 *  <li>after the geometries, the quadtree is stored</li>
 * </ul>
 * 
 * <p>The quadtree stores the envelope of a geometry with the 
 * int array of the [position of geom on disk, size of the geom on disk].
 * It is therefore possible to extract any geometry by knowing the 
 * envelope.
 * </p>
 * 
 * <p>Example write usage:
 * 
 * <pre>
 * WKTReader r = new WKTReader();
 * Geometry pol = r.read("POLYGON ((210 350, 230 310, 290 350, 290 350, 210 350))");
 * pol.setUserData(1);
 * Geometry line = r.read("LINESTRING (50 380, 90 210, 180 160, 240 40, 240 40)");
 * line.setUserData(2);
 * Geometry point = r.read("POINT (130 120)");
 * point.setUserData(3);
 *
 * DiskTreeWriter writer = new DiskTreeWriter("/home/moovida/TMP/index.bin");
 * writer.writeGeometries(new Geometry[]{pol, line, point});
 * </pre>
 * </p>
 *
 * <p>Example read usage:
 * <pre>
 * DiskTreeReader writer = new DiskTreeReader("/home/moovida/TMP/index.bin");
 * Quadtree indexObj = writer.readIndex();
 *
 * List queryAll = indexObj.queryAll();
 *
 * for( Object object : queryAll ) {
 *     if (object instanceof long[]) {
 *         long[] posSize = (long[]) object;
 *         Geometry geom = writer.pickGeometry(posSize[0], posSize[1]);
 *         System.out.println(geom.toText());
 *     }
 * }
 * </pre>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IDiskTree {
    /**
     * Position of the index address.
     */
    long INDEX_ADDRESS_POSITION = 14;

    /**
     * Byte size of the index address.
     */
    long INDEX_ADDRESS_SIZE = 8;

    /**
     * Byte size of the index length.
     */
    long INDEX_LENGTH_SIZE = 8;
}
