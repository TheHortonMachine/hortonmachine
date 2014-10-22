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
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureExtender;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description(OmsLW06_SlopeToNetworkAdder.DESCRIPTION)
@Author(name = OmsLW06_SlopeToNetworkAdder.AUTHORS, contact = OmsLW06_SlopeToNetworkAdder.CONTACTS)
@Keywords(OmsLW06_SlopeToNetworkAdder.KEYWORDS)
@Label(OmsLW06_SlopeToNetworkAdder.LABEL)
@Name("_" + OmsLW06_SlopeToNetworkAdder.NAME)
@Status(OmsLW06_SlopeToNetworkAdder.STATUS)
@License(OmsLW06_SlopeToNetworkAdder.LICENSE)
public class OmsLW06_SlopeToNetworkAdder extends JGTModel implements LWFields {

    @Description(inNetPoints_DESCR)
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description(inSlope_DESCR)
    @In
    public GridCoverage2D inSlope = null;

    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    // VARS DOC START
    public static final String outNetPoints_DESCR = "The output points network layer with the additional attribute of local slope.";
    public static final String inSlope_DESCR = "The input slope raster map";
    public static final String inNetPoints_DESCR = "The input hierarchy point network layer.";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw06_slopetonetworkadder";
    public static final String LABEL = JGTConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "network, vector, point, bankflull, width";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Add the local slope attribute to the input channel point layer.";
    // VARS DOC END

    @Execute
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

}
