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
package org.hortonmachine.gears.modules.v.vectorize;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_F_DEFAULT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_IN_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSVECTORIZER_STATUS;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OMSPOINTSVECTORIZER_DESCRIPTION)
@Documentation(OMSPOINTSVECTORIZER_DOCUMENTATION)
@Author(name = OMSPOINTSVECTORIZER_AUTHORNAMES, contact = OMSPOINTSVECTORIZER_AUTHORCONTACTS)
@Keywords(OMSPOINTSVECTORIZER_KEYWORDS)
@Label(OMSPOINTSVECTORIZER_LABEL)
@Name(OMSPOINTSVECTORIZER_NAME)
@Status(OMSPOINTSVECTORIZER_STATUS)
@License(OMSPOINTSVECTORIZER_LICENSE)
public class OmsPointsVectorizer extends HMModel {

    @Description(OMSPOINTSVECTORIZER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSPOINTSVECTORIZER_F_DEFAULT_DESCRIPTION)
    @In
    public String fDefault = "value";

    @Description(OMSPOINTSVECTORIZER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        checkNull(inRaster);
        HMRaster raster = HMRaster.fromGridCoverage(inRaster);

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("pointtype");
        b.setCRS(inRaster.getCoordinateReferenceSystem());
        b.add("the_geom", Point.class);
        b.add(fDefault, Double.class);

        SimpleFeatureType type = b.buildFeatureType();

        GeometryFactory gF = new GeometryFactory();

        DefaultFeatureCollection points = new DefaultFeatureCollection();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        raster.process(pm, "Extracting points...", ( c, r, value, cols, rows ) -> {
            if (raster.isNovalue(value)) {
                return;
            }
            // the center of the cell, in double precision
            Point point = gF.createPoint(raster.getWorld(c, r));
            builder.addAll(new Object[]{point, value});
            points.add(builder.buildFeature(null));
        });
        raster.close();
        outVector = points;
    }

}
