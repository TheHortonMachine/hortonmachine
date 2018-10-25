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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;

/**
 * LINE DXF entity.
 * This class has a static method reading a DXF LINE and adding the new
 * feature to a FeatureCollection
 * @author Micha�l Michaud
 * @version 0.5.0
 */
// History
@SuppressWarnings("nls")
public class DxfLINE extends DxfENTITY {

    public DxfLINE() {
        super("DEFAULT");
    }

    public static DxfGroup readEntity( RandomAccessFile raf,
            DefaultFeatureCollection entities ) throws IOException {

        double x1 = Double.NaN, y1 = Double.NaN, z1 = Double.NaN;
        double x2 = Double.NaN, y2 = Double.NaN, z2 = Double.NaN;
        DxfGroup group;

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(DxfFile.DXF_LINESCHEMA);
        String layer = "";
        String ltype = "";
        Double elevation = new Double(0.0);
        Double thickness = new Double(0.0);
        Integer color = new Integer(256);
        String text = "";
        Double text_height = new Double(0.0);
        String text_style = "";

        while( null != (group = DxfGroup.readGroup(raf)) && group.getCode() != 0 ) {
            if (group.getCode() == 8)
                layer = group.getValue();
            else if (group.getCode() == 6)
                ltype = group.getValue();
            else if (group.getCode() == 39)
                thickness = new Double(group.getDoubleValue());
            else if (group.getCode() == 62)
                color = new Integer(group.getIntValue());
            else if (group.getCode() == 10)
                x1 = group.getDoubleValue();
            else if (group.getCode() == 20)
                y1 = group.getDoubleValue();
            else if (group.getCode() == 30)
                z1 = group.getDoubleValue();
            else if (group.getCode() == 11)
                x2 = group.getDoubleValue();
            else if (group.getCode() == 21)
                y2 = group.getDoubleValue();
            else if (group.getCode() == 31)
                z2 = group.getDoubleValue();
            else {
            }
        }
        if (!Double.isNaN(x1) && !Double.isNaN(y1) && !Double.isNaN(x2) && !Double.isNaN(y2)) {
            Object[] values = new Object[]{
                    gF.createLineString(new Coordinate[]{new Coordinate(x1, y1, z1),
                            new Coordinate(x2, y2, z2)}), layer, ltype, elevation, thickness,
                    color, text, text_height, text_style};
            builder.addAll(values);
            StringBuilder featureId = new StringBuilder();
            featureId.append(DxfFile.DXF_LINESCHEMA.getTypeName());
            featureId.append(".");
            featureId.append(DxfFile.getNextFid());
            SimpleFeature feature = builder.buildFeature(featureId.toString());
            entities.add(feature);
        }
        return group;
    }

}
