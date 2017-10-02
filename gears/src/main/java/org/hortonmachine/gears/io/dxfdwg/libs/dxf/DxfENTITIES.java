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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;

/**
 * The ENTITIES section of a DXF file containing all data itself
 * @author Micha�l Michaud
 * @version 0.5.0
 */
// History
@SuppressWarnings("nls")
public class DxfENTITIES {
    DefaultFeatureCollection pointEntities;
    DefaultFeatureCollection lineEntities;
    DefaultFeatureCollection polygonEntities;

    public DxfENTITIES() {
        pointEntities = new DefaultFeatureCollection();
        lineEntities = new DefaultFeatureCollection();
        polygonEntities = new DefaultFeatureCollection();
    }

    public static DxfENTITIES readEntities( RandomAccessFile raf ) throws IOException {
        DxfENTITIES dxfEntities = new DxfENTITIES();
        try {
            DxfGroup group = new DxfGroup(2, "ENTITIES");
            String nomVariable;
            while( !group.equals(DxfFile.ENDSEC) ) {
                // System.out.println("Group " + group.getCode() + " " + group.getValue());
                if (group.getCode() == 0) {
                    if (group.getValue().equals("POINT")) {
                        group = DxfPOINT.readEntity(raf, dxfEntities.pointEntities);
                    } else if (group.getValue().equals("TEXT")) {
                        group = DxfTEXT.readEntity(raf, dxfEntities.pointEntities);
                    } else if (group.getValue().equals("LINE")) {
                        group = DxfLINE.readEntity(raf, dxfEntities.lineEntities);
                    } else if (group.getValue().equals("POLYLINE")) {
                        group = DxfPOLYLINE.readEntity(raf, dxfEntities.lineEntities);
                    } else if (group.getValue().equals("LWPOLYLINE")) {
                        group = DxfLWPOLYLINE.readEntity(raf, dxfEntities.lineEntities);
                    } else {
                        group = DxfGroup.readGroup(raf);
                    }
                } else {
                    System.out.println("Group " + group.getCode() + " " + group.getValue() + " UNKNOWN");
                    group = DxfGroup.readGroup(raf);
                }
            }
        } catch (IOException ioe) {
            throw ioe;
        }
        return dxfEntities;
    }

    // public String toString() {
    // Iterator it = entities.iterator();
    // Feature feature;
    // StringBuffer sb = new StringBuffer(DxfFile.SECTION.toString());
    // sb.append(DxfFile.ENTITIES.toString());
    // while( it.hasNext() ) {
    // feature = (Feature) it.next();
    // sb.append(DxfENTITY.feature2Dxf(feature, "LAYER0", true));
    // }
    // sb.append(DxfFile.ENDSEC.toString());
    // return sb.toString();
    // }
    //
    // public void write( BufferedWriter bw, String defaultLayer ) throws IOException {
    // Iterator it = entities.iterator();
    // Feature feature;
    // StringBuffer sb = new StringBuffer(DxfFile.SECTION.toString());
    // sb.append(DxfFile.ENTITIES.toString());
    // try {
    // bw.write(sb.toString());
    // while( it.hasNext() ) {
    // feature = (Feature) it.next();
    // bw.write(DxfENTITY.feature2Dxf(feature, defaultLayer, true));
    // }
    // sb.append(DxfFile.ENDSEC.toString());
    // bw.write(DxfFile.ENDSEC.toString());
    // } catch (IOException ioe) {
    // throw ioe;
    // }
    // }

}
