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
package org.jgrasstools.hortonmachine.modules.networktools.trento_p;

import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Utility.makeLineStringShp;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Utility.makePolygonShp;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Utility.makeShp;

import java.io.File;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.ITrentoPType;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.TrentoPFeatureType;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.LineString;

@Description("Generates the input shapefiles for  a TrentoP simulation.")
@Author(name = "Daniele Andreis")
@Keywords("TrentoP")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TrentoPProjectFilesGenerator extends JGTModel {

    @Description("If it is 0 then create a project file (default mode) otherwise create the calibration shp")
    @In
    public Integer pTest = null;

    @Description("If it is true then generate it from an old output file")
    @In
    public Boolean pExistOldShp = false;

    @Description("The folder into which to create the base files.")
    @In
    public String inFolder = null;

    @Description("The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The name of the .shp file. By deafault it is network.shp")
    @In
    public String pShapeFileName = null;

    @Description("the output fc of TrentoP")
    @In
    public SimpleFeatureCollection pOldFC = null;

    @Description("The name of the .shp file. By deafault it is network.shp")
    @In
    public String pAreaShapeFileName = Constants.AREA_NAME_SHP;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Execute
    public void process() throws Exception {
        // if the test isn't setted then set it to project mode.
        if (pTest == null) {
            pTest = 0;
        }

        // verify if the file name is setted otherwise set it to a default value.
        if (pShapeFileName == null) {
            if (pTest == 0) {
                pShapeFileName = Constants.NETWORK_PROJECT_NAME_SHP;
            } else if (pTest == 1) {
                pShapeFileName = Constants.NETWORK_CALIBRATION_NAME_SHP;
            }
        }

        checkNull(inFolder, pCode);
        CoordinateReferenceSystem crs = CRS.decode(pCode);

        File baseFolder = new File(inFolder);

        pm.beginTask("Create TrentoP project shapefiles...", 7);
        pm.worked(1);
        // if you want to create an empty file
        if (!pExistOldShp) {
            ITrentoPType[] values = PipesTrentoP.values();
            // project
            if (pTest == 0) {
                makeShp(getProjectType(crs), baseFolder, pShapeFileName, null);

            } else if (pTest == 1) {
                // calibration
                makeShp(getCalibrationType(crs), baseFolder, pShapeFileName, null);

            }
            makePolygonShp(values, baseFolder, crs, pAreaShapeFileName);
        } else if (pExistOldShp) {
            if (pOldFC == null) {
                throw new IllegalArgumentException("No old feature collection");
            }
            SimpleFeatureCollection calibrationFC = createNewCollection(getCalibrationType(crs));
            makeShp(calibrationFC.getSchema(), baseFolder, pShapeFileName, calibrationFC);
        }

        pm.done();
    }

    private SimpleFeatureCollection createNewCollection( SimpleFeatureType simpleFeatureType ) {
        SimpleFeatureCollection featureCollection = FeatureCollections.newCollection();
        SimpleFeatureIterator stationsIter = pOldFC.features();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);

        // create the features.
        try {
            int t;

            while( stationsIter.hasNext() ) {
                SimpleFeature networkFeature = stationsIter.next();
                try {
                    // add the geometry.
                    builder.add(networkFeature.getDefaultGeometry());
                    // add the ID.
                    Integer field = ((Integer) networkFeature.getAttribute(TrentoPFeatureType.ID_STR));
                    if (field == null) {

                        throw new IllegalArgumentException();
                    }
                    builder.add(field);
                    // add the ID where this pipe drain.
                    field = ((Integer) networkFeature.getAttribute(TrentoPFeatureType.ID_PIPE_WHERE_DRAIN_STR));
                    if (field == null) {

                        throw new IllegalArgumentException();
                    }
                    builder.add(field);
                    // add the area.
                    Double value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.DRAIN_AREA_STR));
                    if (value == null) {

                        throw new IllegalArgumentException();
                    }
                    builder.add(value);

                    // the pipes elevation is the elevation of the
                    // terrain minus the depth.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.INITIAL_ELEVATION_STR))
                            + ((Double) networkFeature.getAttribute(TrentoPFeatureType.DEPTH_INITIAL_PIPE_STR));

                    builder.add(value);
                    // the pipes elevation is the elevation of the
                    // terrain minus the depth.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.FINAL_ELEVATION_STR))
                            + ((Double) networkFeature.getAttribute(TrentoPFeatureType.DEPTH_FINAL_PIPE_STR));

                    builder.add(value);
                    // add the runoff coefficent.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.RUNOFF_COEFFICIENT_STR));
                    builder.add(value);
                    // add the average residence time.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.AVERAGE_RESIDENCE_TIME_STR));
                    builder.add(value);
                    // add the ks.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.KS_STR));
                    builder.add(value);
                    // add the average slope.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.AVERAGE_SLOPE_STR));
                    builder.add(value);
                    // add the diameters.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.DIAMETER_STR));
                    builder.add(value);
                    // build the feature
                    SimpleFeature feature = builder.buildFeature(null);
                    featureCollection.add(feature);
                } catch (NullPointerException e) {
                    throw new IllegalArgumentException();

                }
            }

        }

        finally {
            stationsIter.close();

        }

        return featureCollection;

    }

    /**
     * Build the Calibration Type.
     * 
     * @param crs
     * @return the type for the calibration shp.
     */
    private SimpleFeatureType getCalibrationType( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        ITrentoPType[] values = TrentoPFeatureType.PipesTrentoP.values();
        String typeName = values[0].getName();
        b.setName(typeName);
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        b.add(values[0].getAttributeName(), values[0].getClazz());
        b.add(values[1].getAttributeName(), values[1].getClazz());
        b.add(values[2].getAttributeName(), values[2].getClazz());
        b.add(values[3].getAttributeName(), values[3].getClazz());
        b.add(values[4].getAttributeName(), values[4].getClazz());
        b.add(values[5].getAttributeName(), values[5].getClazz());
        b.add(values[6].getAttributeName(), values[6].getClazz());
        b.add(values[7].getAttributeName(), values[7].getClazz());
        b.add(values[10].getAttributeName(), values[10].getClazz());
        b.add(values[11].getAttributeName(), values[11].getClazz());
        return b.buildFeatureType();
    }
    /**
     * Build the Project Type.
     * 
     * @param crs
     * @return the type for the calibration shp.
     */
    private SimpleFeatureType getProjectType( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        ITrentoPType[] values = TrentoPFeatureType.PipesTrentoP.values();
        String typeName = values[0].getName();
        b.setName(typeName);
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        b.add(values[0].getAttributeName(), values[0].getClazz());
        b.add(values[1].getAttributeName(), values[1].getClazz());
        b.add(values[2].getAttributeName(), values[2].getClazz());
        b.add(values[3].getAttributeName(), values[3].getClazz());
        b.add(values[4].getAttributeName(), values[4].getClazz());
        b.add(values[5].getAttributeName(), values[5].getClazz());
        b.add(values[6].getAttributeName(), values[6].getClazz());
        b.add(values[7].getAttributeName(), values[7].getClazz());
        b.add(values[8].getAttributeName(), values[8].getClazz());
        b.add(values[9].getAttributeName(), values[9].getClazz());
        b.add(values[10].getAttributeName(), values[10].getClazz());
        return b.buildFeatureType();
    }
}
