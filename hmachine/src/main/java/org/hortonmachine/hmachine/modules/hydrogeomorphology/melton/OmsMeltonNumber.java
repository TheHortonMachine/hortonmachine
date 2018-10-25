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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.melton;

import static java.lang.Math.sqrt;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_fId_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_inFans_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSMELTONNUMBER_outMelton_DESCRIPTION;

import java.text.MessageFormat;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeType;

import org.locationtech.jts.geom.Geometry;

@Description(OMSMELTONNUMBER_DESCRIPTION)
@Author(name = OMSMELTONNUMBER_AUTHORNAMES, contact = OMSMELTONNUMBER_AUTHORCONTACTS)
@Keywords(OMSMELTONNUMBER_KEYWORDS)
@Label(OMSMELTONNUMBER_LABEL)
@Name(OMSMELTONNUMBER_NAME)
@Status(OMSMELTONNUMBER_STATUS)
@License(OMSMELTONNUMBER_LICENSE)
public class OmsMeltonNumber extends HMModel {

    @Description(OMSMELTONNUMBER_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSMELTONNUMBER_inFans_DESCRIPTION)
    @In
    public SimpleFeatureCollection inFans = null;

    @Description(OMSMELTONNUMBER_fId_DESCRIPTION)
    @In
    public String fId;

    @Description(OMSMELTONNUMBER_outMelton_DESCRIPTION)
    @Out
    public String[][] outMelton = null;

    @Execute
    public void process() throws Exception {
        checkNull(inElev, inFans);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double west = regionMap.getWest();
        double east = regionMap.getEast();
        double south = regionMap.getSouth();
        double north = regionMap.getNorth();

        AttributeType type = inFans.getSchema().getType(fId);
        if (type == null) {
            throw new ModelsIllegalargumentException(MessageFormat.format("The attribute {0} does not exist in the vector map.",
                    fId), this, pm);
        }

        List<SimpleFeature> fansList = FeatureUtilities.featureCollectionToList(inFans);

        outMelton = new String[fansList.size()][2];

        int index = 0;
        pm.beginTask("Calculating Melton number for fans...", fansList.size());
        for( SimpleFeature fan : fansList ) {
            Object attribute = fan.getAttribute(fId);

            // rasterize the fan
            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
            newCollection.add(fan);

            OmsScanLineRasterizer rasterizer = new OmsScanLineRasterizer();
            rasterizer.inVector = newCollection;
            rasterizer.pCols = cols;
            rasterizer.pRows = rows;
            rasterizer.pNorth = north;
            rasterizer.pSouth = south;
            rasterizer.pEast = east;
            rasterizer.pWest = west;
            rasterizer.pValue = 1.0;
            rasterizer.pm = new DummyProgressMonitor();
            rasterizer.process();

            GridCoverage2D rasterizedFan = rasterizer.outRaster;

            GridCoverage2D fanElev = CoverageUtilities.coverageValuesMapper(inElev, rasterizedFan);

            // extract min and max
            OmsRasterSummary summary = new OmsRasterSummary();
            summary.pm = new DummyProgressMonitor();
            summary.inRaster = fanElev;
            summary.process();

            double min = summary.outMin;
            double max = summary.outMax;

            // get the suface of the fan
            Geometry geometry = (Geometry) fan.getDefaultGeometry();
            double area = geometry.getArea();

            // calculate Melton
            double melton = (max - min) / sqrt(area);

            outMelton[index][0] = attribute.toString();
            outMelton[index][1] = String.valueOf(melton);
            index++;

            pm.message(MessageFormat.format("id: {0} gave Melton number: {1}", attribute.toString(), melton));
            pm.message("Based on max: " + max + " min: " + min + " and area: " + area);

            pm.worked(1);
        }
        pm.done();

    }

}
