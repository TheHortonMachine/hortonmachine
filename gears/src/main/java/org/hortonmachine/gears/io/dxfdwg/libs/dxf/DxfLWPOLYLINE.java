/*
 * Library name : dxf
 * (C) 2006 Micha�l Michaud
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * michael.michaud@free.fr
 *
 */

package org.hortonmachine.gears.io.dxfdwg.libs.dxf;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

/**
 * LWPOLYLINE DXF entity.
 * This class has a static method reading a DXF LWPOLYLINE and adding the new
 * feature to a FeatureCollection
 * @author Micha�l Michaud
 * @version 0.6.0
 */
// History
@SuppressWarnings("nls")
public class DxfLWPOLYLINE extends DxfENTITY {

    public DxfLWPOLYLINE() {
        super("DEFAULT");
    }

    public static DxfGroup readEntity( RandomAccessFile raf, DefaultFeatureCollection entities )
            throws IOException {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(DxfFile.DXF_LINESCHEMA);
        String layer = "";
        String ltype = "";
        Double elevation = new Double(0.0);
        Double thickness = new Double(0.0);
        Integer color = new Integer(256);
        String text = "";
        Double text_height = new Double(0.0);
        String text_style = "";

        String geomType = "LineString";
        CoordinateList coordList = new CoordinateList();
        double x = Double.NaN, y = Double.NaN, z = Double.NaN;
        DxfGroup group = DxfFile.ENTITIES;
        try {
            while( /*!group.equals(DxfFile.ENDSEC)*/group.getCode() != 0 ) {
                if (group.getCode() == 8) {
                    layer = group.getValue();
                    group = DxfGroup.readGroup(raf);
                } else if (group.getCode() == 6) {
                    ltype = group.getValue();
                    group = DxfGroup.readGroup(raf);
                } else if (group.getCode() == 38) {
                    elevation = new Double(group.getDoubleValue());
                    z = group.getDoubleValue();
                    group = DxfGroup.readGroup(raf);
                } else if (group.getCode() == 39) {
                    thickness = new Double(group.getDoubleValue());
                    group = DxfGroup.readGroup(raf);
                } else if (group.getCode() == 62) {
                    color = new Integer(group.getIntValue());
                    group = DxfGroup.readGroup(raf);
                } else if (group.getCode() == 70) {
                    if ((group.getIntValue() & 1) == 1)
                        geomType = "Polygon";
                    group = DxfGroup.readGroup(raf);
                }
                /*else if (group.equals(VERTEX)) {
                    group = DxfVERTEX.readEntity(raf, coordList);
                }*/
                else if (group.getCode() == 10) {
                    x = group.getDoubleValue();
                    group = DxfGroup.readGroup(raf);
                } else if (group.getCode() == 20) {
                    y = group.getDoubleValue();
                    coordList.add(new Coordinate(x, y, z));
                    group = DxfGroup.readGroup(raf);
                } else if (group.equals(SEQEND)) {
                    group = DxfGroup.readGroup(raf);
                } else if (group.getCode() == 0) {
                    // 0 group different from VERTEX and different from SEQEND
                    break;
                } else {
                    group = DxfGroup.readGroup(raf);
                }
            }
            if (geomType.equals("LineString") || coordList.size() < 4) {
                LineString lineString = gF.createLineString(coordList.toCoordinateArray());
                Object[] values = new Object[]{lineString, layer, ltype, elevation, thickness, color, text, text_height,
                        text_style};
                builder.addAll(values);
                StringBuilder featureId = new StringBuilder();
                featureId.append(DxfFile.DXF_LINESCHEMA.getTypeName());
                featureId.append(".");
                featureId.append(DxfFile.getNextFid());
                SimpleFeature feature = builder.buildFeature(featureId.toString());
                entities.add(feature);
            } else if (geomType.equals("Polygon")) {
                coordList.closeRing();
                Polygon polygon = gF.createPolygon(gF.createLinearRing(coordList.toCoordinateArray()), null);
                Object[] values = new Object[]{polygon, layer, ltype, elevation, thickness, color, text, text_height, text_style};
                builder.addAll(values);
                StringBuilder featureId = new StringBuilder();
                featureId.append(DxfFile.DXF_LINESCHEMA.getTypeName());
                featureId.append(".");
                featureId.append(DxfFile.getNextFid());
                SimpleFeature feature = builder.buildFeature(featureId.toString());
                entities.add(feature);
            } else {
                System.out.println("Can't handle geometry type: " + geomType);
            }
        } catch (IOException ioe) {
            throw ioe;
        }
        return group;
    }

}
