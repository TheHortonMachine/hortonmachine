/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.melton;

import static java.lang.Math.sqrt;

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
import org.geotools.feature.FeatureCollections;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.modules.r.scanline.ScanLineRasterizer;
import org.jgrasstools.gears.modules.r.summary.RasterSummary;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeType;

import com.vividsolutions.jts.geom.Geometry;

@Description("Melton number calculator")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("Melton, Raster, Vector")
@Name("meltonnum")
@Label(JGTConstants.HYDROGEOMORPHOLOGY)
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class MeltonNumber extends JGTModel {

    @Description("The map of elevation.")
    @In
    public GridCoverage2D inElev = null;

    @Description("The map of polygons of the fans.")
    @In
    public SimpleFeatureCollection inFans = null;

    @Description("The fields of the polygons containing the id of the polygon.")
    @In
    public String fId;

    @Description("The Melton numbers per id [id, num].")
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
                    fId), this);
        }

        List<SimpleFeature> fansList = FeatureUtilities.featureCollectionToList(inFans);

        outMelton = new String[fansList.size()][2];

        int index = 0;
        pm.beginTask("Calculating Melton number for fans...", fansList.size());
        for( SimpleFeature fan : fansList ) {
            Object attribute = fan.getAttribute(fId);

            // rasterize the fan
            SimpleFeatureCollection newCollection = FeatureCollections.newCollection();
            newCollection.add(fan);

            ScanLineRasterizer rasterizer = new ScanLineRasterizer();
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
            RasterSummary summary = new RasterSummary();
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
