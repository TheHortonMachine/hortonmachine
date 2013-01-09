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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_doFromold_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_inFolder_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_pCode_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_pMode_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_pNetname_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_pOldVector_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSTRENTOPPROJECTFILESGENERATOR_pShapeAreeName_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Utility.makePolygonShp;

import java.io.File;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.shapefile.OmsShapefileFeatureWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.ITrentoPType;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.TrentoPFeatureType;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.LineString;

@Description(OMSTRENTOPPROJECTFILESGENERATOR_DESCRIPTION)
@Author(name = OMSTRENTOPPROJECTFILESGENERATOR_AUTHORNAMES, contact = OMSTRENTOPPROJECTFILESGENERATOR_AUTHORCONTACTS)
@Keywords(OMSTRENTOPPROJECTFILESGENERATOR_KEYWORDS)
@Label(OMSTRENTOPPROJECTFILESGENERATOR_LABEL)
@Name(OMSTRENTOPPROJECTFILESGENERATOR_NAME)
@Status(OMSTRENTOPPROJECTFILESGENERATOR_STATUS)
@License(OMSTRENTOPPROJECTFILESGENERATOR_LICENSE)
public class OmsTrentoPProjectFilesGenerator extends JGTModel {

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_inFolder_DESCRIPTION)
    @In
    public String inFolder = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_doFromold_DESCRIPTION)
    @In
    public boolean doFromold = false;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pNetname_DESCRIPTION)
    @In
    public String pNetname = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pOldVector_DESCRIPTION)
    @In
    public SimpleFeatureCollection pOldVector = null;

    @Description(OMSTRENTOPPROJECTFILESGENERATOR_pShapeAreeName_DESCRIPTION)
    @In
    public String pShapeAreeName = Constants.AREA_NAME_SHP;

    /**
     * Message handler.
     */
    private final HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {

        // verify if the file name is setted otherwise set it to a default value.
        if (pNetname == null) {
            if (pMode == 0) {
                pNetname = Constants.NETWORK_PROJECT_NAME_SHP;
            } else if (pMode == 1) {
                pNetname = Constants.NETWORK_CALIBRATION_NAME_SHP;
            }
        }

        checkNull(inFolder, pCode);
        CoordinateReferenceSystem crs = CRS.decode(pCode);
        pm.beginTask(msg.message("trentoP.generatefile.project"), -1);
        pm.worked(1);
        // if you want to create an empty file
        if (!doFromold) {
            ITrentoPType[] values = PipesTrentoP.values();
            String file = new File(inFolder, pNetname).getAbsolutePath();
            if (pMode == 0) {
                // project
                OmsShapefileFeatureWriter.writeEmptyShapefile(file, getProjectFeatureType(crs));
            } else if (pMode == 1) {
                // calibration
                OmsShapefileFeatureWriter.writeEmptyShapefile(file, getCalibrationFeatureType(crs));
            }
            file = new File(inFolder, pShapeAreeName).getAbsolutePath();
            makePolygonShp(values, file, crs, pShapeAreeName);
        } else if (doFromold) {
            if (pOldVector == null) {
                throw new IllegalArgumentException(msg.message("trentoP.generatefile.error.noFeature"));
            }
            String file = new File(inFolder, pNetname).getAbsolutePath();
            SimpleFeatureCollection calibrationFC = createNewCollection(getCalibrationFeatureType(crs));
            OmsShapefileFeatureWriter.writeShapefile(file, calibrationFC);
        }
        pm.done();
    }

    private SimpleFeatureCollection createNewCollection( SimpleFeatureType simpleFeatureType ) {
        SimpleFeatureCollection featureCollection = FeatureCollections.newCollection();
        SimpleFeatureIterator stationsIter = pOldVector.features();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(simpleFeatureType);

        // create the features.
        try {
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

                    // add the area.
                    Double value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.DRAIN_AREA_STR));
                    if (value == null) {

                        throw new IllegalArgumentException();
                    }
                    builder.add(value);
                    // add the percentage of the area which is dry.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.PERCENTAGE_OF_DRY_AREA));
                    builder.add(value);
                    // the pipes elevation is the elevation of the
                    // terrain minus the depth.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.DEPTH_INITIAL_PIPE_STR));
                    builder.add(value);
                    // the pipes elevation is the elevation of the
                    // terrain minus the depth.
                    value = ((Double) networkFeature.getAttribute(TrentoPFeatureType.DEPTH_FINAL_PIPE_STR));
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

        } finally {
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
    private SimpleFeatureType getCalibrationFeatureType( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        ITrentoPType[] values = TrentoPFeatureType.PipesTrentoP.values();
        String typeName = values[0].getName();
        b.setName(typeName);
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        // create ID attribute.
        b.add(values[0].getAttributeName(), values[0].getClazz());
        // create drain area attribute.
        b.add(values[2].getAttributeName(), values[2].getClazz());
        // create the percentage area.
        b.add(values[11].getAttributeName(), values[12].getClazz());
        // The upstream elevation of the node.
        b.add(values[3].getAttributeName(), values[3].getClazz());
        // The downstream elevation of the land.
        b.add(values[4].getAttributeName(), values[4].getClazz());
        // runoff coefficent.
        b.add(values[5].getAttributeName(), values[5].getClazz());
        // average residence time.
        b.add(values[6].getAttributeName(), values[6].getClazz());
        // ks
        b.add(values[7].getAttributeName(), values[7].getClazz());
        // average slope
        b.add(values[10].getAttributeName(), values[10].getClazz());
        // diameter to verify
        b.add(values[19].getAttributeName(), values[11].getClazz());
        return b.buildFeatureType();
    }
    /**
     * Build the Project Type.
     * 
     * @param crs
     * @return the type for the calibration shp.
     */
    private SimpleFeatureType getProjectFeatureType( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        ITrentoPType[] values = TrentoPFeatureType.PipesTrentoP.values();
        String typeName = values[0].getName();
        b.setName(typeName);
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        // create ID attribute.
        b.add(values[0].getAttributeName(), values[0].getClazz());
        // create drain area attribute.
        b.add(values[2].getAttributeName(), values[2].getClazz());
        // create the percentage area.
        b.add(values[11].getAttributeName(), values[12].getClazz());
        // The upstream elevation of the land.
        b.add(values[3].getAttributeName(), values[3].getClazz());
        // The downstream elevation of the land.
        b.add(values[4].getAttributeName(), values[4].getClazz());
        // runoff coefficent.
        b.add(values[5].getAttributeName(), values[5].getClazz());
        // average residence time.
        b.add(values[6].getAttributeName(), values[6].getClazz());
        // ks
        b.add(values[7].getAttributeName(), values[7].getClazz());
        // minimum slope.
        b.add(values[8].getAttributeName(), values[8].getClazz());
        // section type
        b.add(values[9].getAttributeName(), values[9].getClazz());
        // average slope of the basin.
        b.add(values[10].getAttributeName(), values[10].getClazz());
        return b.buildFeatureType();
    }
}
