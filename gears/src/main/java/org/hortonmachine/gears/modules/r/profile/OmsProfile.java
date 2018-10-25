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
package org.hortonmachine.gears.modules.r.profile;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.coverage.ProfilePoint;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

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

@Description(OmsProfile.OMSPROFILE_DESCRIPTION)
@Documentation(OmsProfile.OMSPROFILE_DOCUMENTATION)
@Author(name = OmsProfile.OMSPROFILE_AUTHORNAMES, contact = OmsProfile.OMSPROFILE_AUTHORCONTACTS)
@Keywords(OmsProfile.OMSPROFILE_KEYWORDS)
@Label(OmsProfile.OMSPROFILE_LABEL)
@Name(OmsProfile.OMSPROFILE_NAME)
@Status(OmsProfile.OMSPROFILE_STATUS)
@License(OmsProfile.OMSPROFILE_LICENSE)
public class OmsProfile extends HMModel {

    @Description(OMSPROFILE_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSPROFILE_IN_COORDINATES_DESCRIPTION)
    @In
    public String inCoordinates;

    @Description(OMSPROFILE_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSPROFILE_F_LINE_ID_DESCRIPTION)
    @In
    public String fLineid;

    @Description(OMSPROFILE_OUT_FOLDER_DESCRIPTION)
    @In
    public String outFolder;

    @Description(OMSPROFILE_OUT_PROFILE_DESCRIPTION)
    @Out
    public double[][] outProfile;
    
    public static final String OMSPROFILE_DESCRIPTION = "Module creating profiles over rasters.";
    public static final String OMSPROFILE_DOCUMENTATION = "";
    public static final String OMSPROFILE_KEYWORDS = "OmsProfile, Raster";
    public static final String OMSPROFILE_LABEL = RASTERPROCESSING;
    public static final String OMSPROFILE_NAME = "profile";
    public static final int OMSPROFILE_STATUS = 5;
    public static final String OMSPROFILE_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSPROFILE_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSPROFILE_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSPROFILE_IN_RASTER_DESCRIPTION = "The raster map to use for the profile";
    public static final String OMSPROFILE_IN_COORDINATES_DESCRIPTION = "Comma separated list of easting,northing coordinates to trace the profile on (optional).";
    public static final String OMSPROFILE_IN_VECTOR_DESCRIPTION = "Line vector map to use to trace the profile on (optional).";
    public static final String OMSPROFILE_F_LINE_ID_DESCRIPTION = "The id of the line to use for the name of the profile output file name (used in case of inVector use).";
    public static final String OMSPROFILE_OUT_FOLDER_DESCRIPTION = "The folder in which to place the output profiles if multiple (used in case of inVector use).";
    public static final String OMSPROFILE_OUT_PROFILE_DESCRIPTION = "The output profile for the last line read (contains progressive, elevation, x, y).";


    @Execute
    public void process() throws Exception {
        checkNull(inRaster);

        if (inCoordinates == null && inVector == null) {
            throw new ModelsIllegalargumentException(
                    "Either the coordinates or a vector map to trace the profile on have to be supplied.", this, pm);
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
            if (EGeometryType.isLine(geom)) {
                Coordinate[] coordinates = geom.getCoordinates();
                for( Coordinate coordinate : coordinates ) {
                    profileNodesList.add(coordinate);
                }
            } else {
                throw new ModelsIllegalargumentException("The module works only for lines.", this, pm);
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
                    Coordinate coord = profilePoint.getPosition();
                    sb.append(progressive).append(", ").append(elev).append(", ").append(coord.x).append(", ").append(coord.y)
                            .append("\n");
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
                    this, pm);
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
