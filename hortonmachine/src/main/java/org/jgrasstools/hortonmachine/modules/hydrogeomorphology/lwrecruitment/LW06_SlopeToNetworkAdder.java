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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment;

import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description("Add the local slope attribute to the input channel point layer.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("network, vector, point, bankflull, width")
@Label("HortonMachine/Hydro-Geomorphology/LWRecruitment")
@Name("LW06_SlopeToNetworkAdder")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LW06_SlopeToNetworkAdder extends JGTModel implements LWFields {

    @Description("The input hierarchy point network layer.")
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description("The input slope raster map")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public GridCoverage2D inSlope = null;

    @Description("The output points network layer with the additional attribute of local slope.")
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    public void process() throws Exception {

        // creates the schema for the output shapefile
        FeatureExtender ext = new FeatureExtender(inNetPoints.getSchema(), new String[]{SLOPE}, new Class[]{Double.class});
        pm.beginTask("extract points...", inNetPoints.size());
        /*
         * reads the network
         */
        List<SimpleFeature> netList = FeatureUtilities.featureCollectionToList(inNetPoints);
        outNetPoints = new DefaultFeatureCollection();

        for( SimpleFeature netFeature : netList ) {
            Geometry geometry = (Geometry) netFeature.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();
            
            // gets the slope in the correspondent raster cell
            double slope = CoverageUtilities.getValue(inSlope, coordinate);
            // extents and adds the data to the output FC
            SimpleFeature extendedFeature = ext.extendFeature(netFeature, new Object[]{slope});
            ((DefaultFeatureCollection) outNetPoints).add(extendedFeature);
            pm.worked(1);
        }
        pm.done();

    }
    public static void main( String[] args ) throws Exception {

        String inNetPoints = "D:/lavori_tmp/gsoc/netpoints_width_bridgesdams.shp";
        String inSlope = "D:/lavori_tmp/gsoc/raster/slope.asc";

        String outNetPoints = "D:/lavori_tmp/gsoc/netpoints_width_bridgesdams_slope.shp";

        LW06_SlopeToNetworkAdder slopeToNetworkAdder = new LW06_SlopeToNetworkAdder();
        slopeToNetworkAdder.inNetPoints = OmsVectorReader.readVector(inNetPoints);
        slopeToNetworkAdder.inSlope = OmsRasterReader.readRaster(inSlope);

        slopeToNetworkAdder.process();

        SimpleFeatureCollection outNetPointFC = slopeToNetworkAdder.outNetPoints;
        OmsVectorWriter.writeVector(outNetPoints, outNetPointFC);
    }

}
