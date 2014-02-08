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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_fLineid_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_inCoordinates_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_inRaster_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_outFolder_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPROFILE_outProfile_DESCRIPTION;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.coverage.ProfilePoint;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description(OMSPROFILE_DESCRIPTION)
@Documentation(OMSPROFILE_DOCUMENTATION)
@Author(name = OMSPROFILE_AUTHORNAMES, contact = OMSPROFILE_AUTHORCONTACTS)
@Keywords(OMSPROFILE_KEYWORDS)
@Label(OMSPROFILE_LABEL)
@Name(OMSPROFILE_NAME)
@Status(OMSPROFILE_STATUS)
@License(OMSPROFILE_LICENSE)
public class OmsProfile extends JGTModel {

    @Description(OMSPROFILE_inRaster_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSPROFILE_inCoordinates_DESCRIPTION)
    @In
    public String inCoordinates;

    @Description(OMSPROFILE_inVector_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSPROFILE_fLineid_DESCRIPTION)
    @In
    public String fLineid;

    @Description(OMSPROFILE_outFolder_DESCRIPTION)
    @In
    public String outFolder;

    @Description(OMSPROFILE_outProfile_DESCRIPTION)
    @Out
    public double[][] outProfile;

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
            if (GeometryUtilities.isLine(geom)) {
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
