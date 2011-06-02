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
package org.jgrasstools.gears.modules.r.profile;

import java.util.ArrayList;
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
import org.geotools.data.simple.SimpleFeatureIterator;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.coverage.ProfilePoint;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description("Module creating profiles over rasters.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Profile, Raster")
@Label(JGTConstants.RASTERPROCESSING)
@Name("profile")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class Profile extends JGTModel {

    @Description("The raster map to use for the profile")
    @In
    public GridCoverage2D inRaster;

    @Description("Comma separated list of easting,northing coordinates to trace the profile on (optional).")
    @In
    public String inCoordinates;

    @Description("Line vector map to use to trace the profile on (optional).")
    @In
    public SimpleFeatureCollection inVector;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The output profile.")
    @Out
    public double[][] outProfile;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        if (inCoordinates == null && inVector == null) {
            throw new ModelsIllegalargumentException(
                    "Either the coordinates or a vector map to trace the profile on have to be supplied.", this);
        }

        List<Coordinate> profileNodesList = new ArrayList<Coordinate>();
        if (inCoordinates != null && inCoordinates.length() > 0) {
            try {
                pm.message("Using supplied coordinates to trace the profile...");
                String[] split = inCoordinates.split(",");
                for( int i = 0; i < split.length; i++ ) {
                    double east = Double.parseDouble(split[i].trim());
                    i++;
                    double north = Double.parseDouble(split[i].trim());
                    Coordinate tmp = new Coordinate(east, north);
                    profileNodesList.add(tmp);
                }

            } catch (Exception e) {
                throw new ModelsIllegalargumentException(
                        "A problem occurred while parsing the supplied profile nodes coordinates. Check your syntax.", this);
            }
        } else if (inVector != null) {
            // take just the first feature, we do not do them all
            SimpleFeatureIterator featuresIterator = inVector.features();
            if (featuresIterator.hasNext()) {
                SimpleFeature f = featuresIterator.next();
                Geometry geom = (Geometry) f.getDefaultGeometry();

                if (GeometryUtilities.isLine(geom)) {
                    pm.message("Using supplied vector map to trace the profile...");
                    Coordinate[] coordinates = geom.getCoordinates();
                    for( Coordinate coordinate : coordinates ) {
                        profileNodesList.add(coordinate);
                    }
                } else {
                    throw new ModelsIllegalargumentException("The module works only for lines.", this);
                }
            }
        }

        if (profileNodesList.size() < 2) {
            throw new ModelsIllegalargumentException("We need at least two coordinates to create a profile. Check your syntax.",
                    this);
        }

        List<ProfilePoint> profilePoints = CoverageUtilities.doProfile(inRaster, profileNodesList.toArray(new Coordinate[0]));
        outProfile = new double[profilePoints.size()][4];
        for( int i = 0; i < profilePoints.size(); i++ ) {
            ProfilePoint profilePoint = profilePoints.get(i);
            double progressive = profilePoint.getProgressive();
            double elev = profilePoint.getElevation();
            Coordinate coord = profilePoint.getPosition();
            outProfile[i][0] = progressive;
            outProfile[i][1] = elev;
            outProfile[i][2] = coord.x;
            outProfile[i][3] = coord.y;
        }
    }

}
