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

/**
 * POINT DXF entity.
 * This class has a static method reading a DXF POINT and adding the new
 * feature to a FeatureCollection
 * @author Micha�l Michaud
 * @version 0.5.0
 */
// History
@SuppressWarnings("nls")
public class DxfPOINT extends DxfENTITY {

    public DxfPOINT() {
        super("DEFAULT");
    }

    public static DxfGroup readEntity( RandomAccessFile raf,
            DefaultFeatureCollection entities )
            throws IOException {

        double x = Double.NaN, y = Double.NaN, z = Double.NaN;
        DxfGroup group;
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(DxfFile.DXF_POINTSCHEMA);
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
            else if (group.getCode() == 38)
                elevation = new Double(group.getDoubleValue());
            else if (group.getCode() == 39)
                thickness = new Double(group.getDoubleValue());
            else if (group.getCode() == 62)
                color = new Integer(group.getIntValue());
            else if (group.getCode() == 10)
                x = group.getDoubleValue();
            else if (group.getCode() == 20)
                y = group.getDoubleValue();
            else if (group.getCode() == 30)
                z = group.getDoubleValue();
            else {
            }
        }

        if (!Double.isNaN(x) && !Double.isNaN(y)) {
            Object[] values = new Object[]{gF.createPoint(new Coordinate(x, y, z)), layer, ltype,
                    elevation, thickness, color, text, text_height, text_style};
            builder.addAll(values);
            StringBuilder featureId = new StringBuilder();
            featureId.append(DxfFile.DXF_POINTSCHEMA.getTypeName());
            featureId.append(".");
            featureId.append(DxfFile.getNextFid());
            SimpleFeature feature = builder.buildFeature(featureId.toString());
            entities.add(feature);
        }
        return group;
    }

}
