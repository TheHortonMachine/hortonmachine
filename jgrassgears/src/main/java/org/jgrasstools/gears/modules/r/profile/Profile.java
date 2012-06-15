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

import java.io.File;
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
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.coverage.ProfilePoint;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;

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

    @Description("The id of the line to use for the name of the profile output file name (used in case of inVector use).")
    @In
    public String fLineid;

    @Description("The folder in which to place the output profiles if multiple (used in case of inVector use).")
    @In
    public String outFolder;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The output profile for the last line read (contains progressive, elevation, x, y).")
    @Out
    public double[][] outProfile;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        if (inCoordinates == null && inVector == null) {
            throw new ModelsIllegalargumentException(
                    "Either the coordinates or a vector map to trace the profile on have to be supplied.", this);
        }

        if (inCoordinates != null && inCoordinates.length() > 0) {
            profileFromManualCoordinates();
        } else if (inVector != null) {
            if (inVector != null && inVector.size() != 1)
                checkNull(outFolder, fLineid);
            profileFromFeatureCollection();
        }

    }

    private void profileFromFeatureCollection() throws Exception {

        pm.message("Using supplied vector map to trace the profile...");
        List<FeatureMate> linesList = FeatureUtilities.featureCollectionToMatesList(inVector);
        for( FeatureMate lineFeature : linesList ) {
            Geometry geom = lineFeature.getGeometry();
            List<Coordinate> profileNodesList = new ArrayList<Coordinate>();
            if (GeometryUtilities.isLine(geom)) {
                Coordinate[] coordinates = geom.getCoordinates();
                for( Coordinate coordinate : coordinates ) {
                    profileNodesList.add(coordinate);
                }
            } else {
                throw new ModelsIllegalargumentException("The module works only for lines.", this);
            }
            // dump the profile
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

            if (outFolder != null && fLineid != null) {
                String id = lineFeature.getAttribute(fLineid, String.class);
                File outFolderFile = new File(outFolder);
                File profileFile = new File(outFolderFile, id + ".csv");
                StringBuilder sb = new StringBuilder();
                for( int i = 0; i < profilePoints.size(); i++ ) {
                    ProfilePoint profilePoint = profilePoints.get(i);
                    double progressive = profilePoint.getProgressive();
                    double elev = profilePoint.getElevation();
                    // Coordinate coord = profilePoint.getPosition();
                    sb.append(progressive).append(", ").append(elev).append("\n");
                }
                FileUtilities.writeFile(sb.toString(), profileFile);

                double meanSlope = ProfilePoint.getMeanSlope(profilePoints);
                StringBuilder sbSlope = new StringBuilder();
                sbSlope.append("Mean slope for id = ");
                sbSlope.append(id);
                sbSlope.append(" is ");
                sbSlope.append(meanSlope);
                pm.message(sbSlope.toString());
            } else {
                pm.errorMessage("Evaluating only first feature when writing to console. If you need the profile of all features, define an output folder.");
                break;
            }
        }
    }

    private void profileFromManualCoordinates() throws Exception {
        List<Coordinate> profileNodesList = new ArrayList<Coordinate>();
        pm.message("Using supplied coordinates to trace the profile...");
        String[] split = inCoordinates.split(",");
        for( int i = 0; i < split.length; i++ ) {
            double east = Double.parseDouble(split[i].trim());
            i++;
            double north = Double.parseDouble(split[i].trim());
            Coordinate tmp = new Coordinate(east, north);
            profileNodesList.add(tmp);
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

        double meanSlope = ProfilePoint.getMeanSlope(profilePoints);
        StringBuilder sbSlope = new StringBuilder();
        sbSlope.append("Mean slope profile is ");
        sbSlope.append(meanSlope);
        pm.message(sbSlope.toString());
    }
}
