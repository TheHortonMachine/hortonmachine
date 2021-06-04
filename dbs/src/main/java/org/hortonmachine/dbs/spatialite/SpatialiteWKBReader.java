/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package org.hortonmachine.dbs.spatialite;

import java.io.IOException;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.CoordinateSequences;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ByteArrayInStream;
import org.locationtech.jts.io.ByteOrderDataInStream;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.InStream;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBConstants;
import org.locationtech.jts.io.WKBReader;

/**
 * A modified version of the JTS {@link WKBReader} that supports spatialite geometry blobs.
 * 
 *  <p>Specifications can be found here: https://www.gaia-gis.it/gaia-sins/BLOB-Geometry.html
 * 
 *  @author JTS developers
 *  @author Andrea Antonello (www.hydrologis.com)
 *  @see WKBReader
 */
public class SpatialiteWKBReader {
    /**
     * Converts a hexadecimal string to a byte array.
     * The hexadecimal digit symbols are case-insensitive.
     *
     * @param hex a string containing hex digits
     * @return an array of bytes with the value of the hex string
     */
    public static byte[] hexToBytes( String hex ) {
        int byteLen = hex.length() / 2;
        byte[] bytes = new byte[byteLen];

        for( int i = 0; i < hex.length() / 2; i++ ) {
            int i2 = 2 * i;
            if (i2 + 1 > hex.length())
                throw new IllegalArgumentException("Hex string has odd length");

            int nib1 = hexToInt(hex.charAt(i2));
            int nib0 = hexToInt(hex.charAt(i2 + 1));
            byte b = (byte) ((nib1 << 4) + (byte) nib0);
            bytes[i] = b;
        }
        return bytes;
    }

    private static int hexToInt( char hex ) {
        int nib = Character.digit(hex, 16);
        if (nib < 0)
            throw new IllegalArgumentException("Invalid hex digit: '" + hex + "'");
        return nib;
    }

    private static final String INVALID_GEOM_TYPE_MSG = "Invalid geometry type encountered in ";

    private GeometryFactory factory;
    private CoordinateSequenceFactory csFactory;
    private PrecisionModel precisionModel;
    // default dimension - will be set on read
    private int inputDimension = 2;
    /**
     * true if structurally invalid input should be reported rather than repaired.
     * At some point this could be made client-controllable.
     */
    private boolean isStrict = false;
    private ByteOrderDataInStream dis = new ByteOrderDataInStream();
    private double[] ordValues;

    private int SRID;

    public SpatialiteWKBReader() {
        this(new GeometryFactory());
    }

    public SpatialiteWKBReader( GeometryFactory geometryFactory ) {
        this.factory = geometryFactory;
        precisionModel = factory.getPrecisionModel();
        csFactory = factory.getCoordinateSequenceFactory();
    }

    /**
     * Reads a single {@link Geometry} in WKB format from a byte array.
     *
     * @param bytes the byte array to read from
     * @return the geometry read
     * @throws ParseException if the WKB is ill-formed
     */
    public Geometry read( byte[] bytes ) throws Exception {
        // possibly reuse the ByteArrayInStream?
        // don't throw IOExceptions, since we are not doing any I/O
        try {
            return read(new ByteArrayInStream(bytes));
        } catch (IOException ex) {
            throw new RuntimeException("Unexpected IOException caught: " + ex.getMessage());
        }
    }

    /**
     * Reads a {@link Geometry} in binary WKB format from an {@link InStream}.
     *
     * @param is the stream to read from
     * @return the Geometry read
     * @throws IOException if the underlying stream creates an error
     * @throws ParseException if the WKB is ill-formed
     */
    public Geometry read( InStream is ) throws Exception {
        dis.setInStream(is);
        Geometry g = readSpatialiteGeometry();
        return g;
    }

    @SuppressWarnings("unused")
    private Geometry readSpatialiteGeometry() throws Exception {
        int start = dis.readByte();
        if (start != 0x00) {
            throw new IllegalArgumentException("Not a geometry, start byte != 0x00");
        }
        // determine byte order
        byte byteOrderWKB = dis.readByte();
        // always set byte order, since it may change from geometry to geometry
        int byteOrder = byteOrderWKB == WKBConstants.wkbNDR ? ByteOrderValues.LITTLE_ENDIAN : ByteOrderValues.BIG_ENDIAN;
        dis.setOrder(byteOrder);

        SRID = dis.readInt();

        // 6 - 13 MBR_MIN_X a double value [little- big-endian ordered, accordingly with the
        // precedent one]
        // corresponding to the MBR minimum X coordinate for this GEOMETRY
        // 14 - 21 MBR_MIN_Y a double value corresponding to the MBR minimum Y coordinate
        // 22 - 29 MBR_MAX_X a double value corresponding to the MBR maximum X coordinate
        // 30 - 37 MBR_MAX_Y a double value corresponding to the MBR maximum Y coordinate
         double mbrMinX = dis.readDouble();
         double mbrMinY = dis.readDouble();
         double mbrMaxX = dis.readDouble();
         double mbrMaxY = dis.readDouble();

        // // 38 MBR_END [hex 7C] a GEOMETRY encoded BLOB value must always have an 0x7C byte in
        // this
        // // position
        int mbrEnd = dis.readByte();
        if (mbrEnd != 0x7C) {
            throw new IllegalArgumentException("Not a geometry, MBR_END != 0x7C");
        }

        int typeInt = dis.readInt();
        int geometryType = typeInt & 0xff;
        // determine if Z values are present
        boolean hasZ = (typeInt & 0x80000000) != 0;
        inputDimension = hasZ ? 3 : 2;

        // only allocate ordValues buffer if necessary
        if (ordValues == null || ordValues.length < inputDimension)
            ordValues = new double[inputDimension];

        Geometry geom = null;
        switch( geometryType ) {
        case WKBConstants.wkbPoint:
            geom = readPoint();
            break;
        case WKBConstants.wkbLineString:
            geom = readLineString();
            break;
        case WKBConstants.wkbPolygon:
            geom = readPolygon();
            break;
        case WKBConstants.wkbMultiPoint:
            geom = readMultiPoint();
            break;
        case WKBConstants.wkbMultiLineString:
            geom = readMultiLineString();
            break;
        case WKBConstants.wkbMultiPolygon:
            geom = readMultiPolygon();
            break;
        case WKBConstants.wkbGeometryCollection:
            geom = readGeometryCollection();
            break;
        default:
            throw new ParseException("Unknown WKB type " + geometryType);
        }
        setSRID(geom, SRID);
        return geom;
    }

    private Geometry readGeometry() throws Exception {
//        // determine byte order
//        byte byteOrderWKB = dis.readByte();
//        // always set byte order, since it may change from geometry to geometry
//        int byteOrder = byteOrderWKB == WKBConstants.wkbNDR ? ByteOrderValues.LITTLE_ENDIAN : ByteOrderValues.BIG_ENDIAN;
//        dis.setOrder(byteOrder);
//
//        int typeInt = dis.readInt();
//        int geometryType = typeInt & 0xff;
//        // determine if Z values are present
//        boolean hasZ = (typeInt & 0x80000000) != 0;
//        inputDimension = hasZ ? 3 : 2;
//        // determine if SRIDs are present
//        boolean hasSRID = (typeInt & 0x20000000) != 0;
//
//        int SRID = 0;
//        if (hasSRID) {
//            SRID = dis.readInt();
//        }
//
//        // only allocate ordValues buffer if necessary
//        if (ordValues == null || ordValues.length < inputDimension)
//            ordValues = new double[inputDimension];
        
        int entity = dis.readByte();
        if (entity != 0x69) {
            throw new IllegalArgumentException("Not a geometry, entity != 0x69");
        }
        int geometryType = dis.readInt();
        

        Geometry geom = null;
        switch( geometryType ) {
        case WKBConstants.wkbPoint:
            geom = readPoint();
            break;
        case WKBConstants.wkbLineString:
            geom = readLineString();
            break;
        case WKBConstants.wkbPolygon:
            geom = readPolygon();
            break;
        case WKBConstants.wkbMultiPoint:
            geom = readMultiPoint();
            break;
        case WKBConstants.wkbMultiLineString:
            geom = readMultiLineString();
            break;
        case WKBConstants.wkbMultiPolygon:
            geom = readMultiPolygon();
            break;
        case WKBConstants.wkbGeometryCollection:
            geom = readGeometryCollection();
            break;
        default:
            throw new ParseException("Unknown WKB type " + geometryType);
        }
        setSRID(geom, SRID);
        return geom;
    }

    /**
     * Sets the SRID, if it was specified in the WKB
     *
     * @param g the geometry to update
     * @return the geometry with an updated SRID value, if required
     */
    private Geometry setSRID( Geometry g, int SRID ) {
        if (SRID != 0)
            g.setSRID(SRID);
        return g;
    }

    private Point readPoint() throws Exception {
        CoordinateSequence pts = readCoordinateSequence(1);
        return factory.createPoint(pts);
    }

    private LineString readLineString() throws Exception {
        int size = dis.readInt();
        CoordinateSequence pts = readCoordinateSequenceLineString(size);
        return factory.createLineString(pts);
    }

    private LinearRing readLinearRing() throws Exception {
        int size = dis.readInt();
        CoordinateSequence pts = readCoordinateSequenceRing(size);
        return factory.createLinearRing(pts);
    }

    private Polygon readPolygon() throws Exception {
        int numRings = dis.readInt();
        LinearRing[] holes = null;
        if (numRings > 1)
            holes = new LinearRing[numRings - 1];

        LinearRing shell = readLinearRing();
        for( int i = 0; i < numRings - 1; i++ ) {
            holes[i] = readLinearRing();
        }
        return factory.createPolygon(shell, holes);
    }

    private MultiPoint readMultiPoint() throws Exception {
        int numGeom = dis.readInt();
        Point[] geoms = new Point[numGeom];
        for( int i = 0; i < numGeom; i++ ) {
            Geometry g = readGeometry();
            if (!(g instanceof Point))
                throw new ParseException(INVALID_GEOM_TYPE_MSG + "MultiPoint");
            geoms[i] = (Point) g;
        }
        return factory.createMultiPoint(geoms);
    }

    private MultiLineString readMultiLineString() throws Exception {
        int numGeom = dis.readInt();
        LineString[] geoms = new LineString[numGeom];
        for( int i = 0; i < numGeom; i++ ) {
            Geometry g = readGeometry();
            if (!(g instanceof LineString))
                throw new ParseException(INVALID_GEOM_TYPE_MSG + "MultiLineString");
            geoms[i] = (LineString) g;
        }
        return factory.createMultiLineString(geoms);
    }

    private MultiPolygon readMultiPolygon() throws Exception {
        int numGeom = dis.readInt();
        Polygon[] geoms = new Polygon[numGeom];
        for( int i = 0; i < numGeom; i++ ) {
            Geometry g = readGeometry();
            if (!(g instanceof Polygon))
                throw new ParseException(INVALID_GEOM_TYPE_MSG + "MultiPolygon");
            geoms[i] = (Polygon) g;
        }
        return factory.createMultiPolygon(geoms);
    }

    private GeometryCollection readGeometryCollection() throws Exception {
        int numGeom = dis.readInt();
        Geometry[] geoms = new Geometry[numGeom];
        for( int i = 0; i < numGeom; i++ ) {
            geoms[i] = readGeometry();
        }
        return factory.createGeometryCollection(geoms);
    }

    private CoordinateSequence readCoordinateSequence( int size ) throws Exception {
        CoordinateSequence seq = csFactory.create(size, inputDimension);
        int targetDim = seq.getDimension();
        if (targetDim > inputDimension)
            targetDim = inputDimension;
        for( int i = 0; i < size; i++ ) {
            readCoordinate();
            for( int j = 0; j < targetDim; j++ ) {
                seq.setOrdinate(i, j, ordValues[j]);
            }
        }
        return seq;
    }

    private CoordinateSequence readCoordinateSequenceLineString( int size ) throws Exception {
        CoordinateSequence seq = readCoordinateSequence(size);
        if (isStrict)
            return seq;
        if (seq.size() == 0 || seq.size() >= 2)
            return seq;
        return CoordinateSequences.extend(csFactory, seq, 2);
    }

    private CoordinateSequence readCoordinateSequenceRing( int size ) throws Exception {
        CoordinateSequence seq = readCoordinateSequence(size);
        if (isStrict)
            return seq;
        if (CoordinateSequences.isRing(seq))
            return seq;
        return CoordinateSequences.ensureValidRing(csFactory, seq);
    }

    /**
     * Reads a coordinate value with the specified dimensionality.
     * Makes the X and Y ordinates precise according to the precision model
     * in use.
     */
    private void readCoordinate() throws Exception {
        for( int i = 0; i < inputDimension; i++ ) {
            if (i <= 1) {
                ordValues[i] = precisionModel.makePrecise(dis.readDouble());
            } else {
                ordValues[i] = dis.readDouble();
            }

        }
    }

}