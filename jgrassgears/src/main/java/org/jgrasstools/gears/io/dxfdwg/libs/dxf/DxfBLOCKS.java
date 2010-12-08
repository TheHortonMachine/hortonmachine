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

package org.jgrasstools.gears.io.dxfdwg.libs.dxf;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;


/**
 * A DXF block contains a block of geometries. The dxf driver can read entities
 * inside a block, but it will not remember that the entities are in a same
 * block.
 * @author Micha�l Michaud
 * @version 0.5.0
 */
@SuppressWarnings("nls")
public class DxfBLOCKS {
    //final static FeatureSchema DXF_SCHEMA = new FeatureSchema();
    FeatureCollection<SimpleFeatureType, SimpleFeature> pointEntities;
    FeatureCollection<SimpleFeatureType, SimpleFeature> lineEntities;
    FeatureCollection<SimpleFeatureType, SimpleFeature> polygonEntities;

    public DxfBLOCKS() {
        /*
        DXF_SCHEMA.addAttribute("GEOMETRY", AttributeType.GEOMETRY);
        DXF_SCHEMA.addAttribute("LAYER", AttributeType.STRING);
        DXF_SCHEMA.addAttribute("LTYPE", AttributeType.STRING);
        DXF_SCHEMA.addAttribute("THICKNESS", AttributeType.DOUBLE);
        DXF_SCHEMA.addAttribute("COLOR", AttributeType.INTEGER);
        DXF_SCHEMA.addAttribute("TEXT", AttributeType.STRING);
        */
        pointEntities = FeatureCollections.newCollection();
        lineEntities = FeatureCollections.newCollection();
        polygonEntities = FeatureCollections.newCollection();
    }

    public static DxfBLOCKS readBlocks(RandomAccessFile raf) throws IOException {
        DxfBLOCKS blocks = new DxfBLOCKS();
        try {
            DxfGroup group = null;
            while (null != (group = DxfGroup.readGroup(raf)) &&
                   !group.equals(DxfFile.ENDSEC)) {
            }
        } catch(IOException ioe) {throw ioe;}
        return blocks;
    }

    public static DxfBLOCKS readEntities(RandomAccessFile raf) throws IOException {
        DxfBLOCKS dxfEntities = new DxfBLOCKS();
        try {
            DxfGroup group = new DxfGroup(2, "BLOCKS");
            while (!group.equals(DxfFile.ENDSEC)) {
                 //System.out.println("Group " + group.getCode() + " " + group.getValue());
                 if (group.getCode() == 0) {
                     if (group.getValue().equals("POINT")) {
                         group = DxfPOINT.readEntity(raf, dxfEntities.pointEntities);
                     }
                     else if (group.getValue().equals("TEXT")) {
                         group = DxfTEXT.readEntity(raf, dxfEntities.pointEntities);
                     }
                     else if (group.getValue().equals("LINE")) {
                         group = DxfLINE.readEntity(raf, dxfEntities.lineEntities);
                     }
                     else if (group.getValue().equals("POLYLINE")) {
                         group = DxfPOLYLINE.readEntity(raf, dxfEntities.lineEntities);
                     }
                     else if (group.getValue().equals("TEXT")) {
                         group = DxfTEXT.readEntity(raf, dxfEntities.pointEntities);
                     }
                     else {group = DxfGroup.readGroup(raf);}
                 }
                 else {
                     //System.out.println("Group " + group.getCode() + " " + group.getValue() + " UNKNOWN");
                     group = DxfGroup.readGroup(raf);
                 }
            }
        } catch(IOException ioe) {throw ioe;}
        return dxfEntities;
    }

}
