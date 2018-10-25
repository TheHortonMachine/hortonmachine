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
package org.hortonmachine.lesto.modules.utilities;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.clustering.GvmClusters;
import org.hortonmachine.gears.utils.clustering.GvmResult;
import org.hortonmachine.gears.utils.clustering.GvmVectorSpace;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.lesto.modules.utilities.cluster.LasClusterElevationKeyer;
import org.hortonmachine.lesto.modules.utilities.cluster.ShpClusterElevationKeyer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

@Description("Clustering of point data.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("cluster, lidar, las")
@Label(HMConstants.LESTO + "/utilities")
@Name("lasinfo")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class PointClusterer extends HMModel {
    @Description("Input las or shp file to be clustered.")
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inFile = null;

    @Description("Number of clusters.")
    @In
    public int pClusterCount = 1000;

    @Description("Shapefile field containing the value to cluster.")
    @In
    public String fClusterName = "elev";

    @Description("Clustered output shapefile.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outShp = null;

    private final GvmVectorSpace space = new GvmVectorSpace(2);

    public PointClusterer() throws Exception {
        checkNull(inFile, outShp);
        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        if (inFile.toLowerCase().endsWith(".las")) {
            try (ALasReader lasReader = ALasReader.getReader(new File(inFile), null)) {
                lasReader.open();
                ILasHeader header = lasReader.getHeader();
                CoordinateReferenceSystem crs = header.getCrs();

                GvmClusters<GvmVectorSpace, LasRecord> clusters = new GvmClusters<GvmVectorSpace, LasRecord>(space, pClusterCount);
                clusters.setKeyer(new LasClusterElevationKeyer());
                double[] vector = clusters.getSpace().newOrigin();
                pm.beginTask("Clustering...", (int) header.getRecordsCount());
                while( lasReader.hasNextPoint() ) {
                    LasRecord dot = lasReader.getNextPoint();
                    vector[0] = dot.x;
                    vector[1] = dot.y;
                    clusters.add(dot.z, vector, dot);
                    pm.worked(1);
                }
                pm.done();

                SimpleFeatureBuilder builder = getfeatureBuilder(crs);

                List<GvmResult<LasRecord>> results = clusters.results();
                for( GvmResult<LasRecord> gvmResult : results ) {
                    int count = gvmResult.getCount();
                    double mass = gvmResult.getMass();
                    double variance = gvmResult.getVariance();
                    double stdev = gvmResult.getStdDeviation();
                    double[] point = (double[]) gvmResult.getPoint();
                    double avgElev = mass / count;
                    Point p = gf.createPoint(new Coordinate(point[0], point[1]));
                    Object[] values = new Object[]{p, count, mass, variance, stdev, avgElev};
                    builder.addAll(values);
                    SimpleFeature feature = builder.buildFeature(null);
                    newCollection.add(feature);
                }
            }
        } else if (inFile.toLowerCase().endsWith(".shp")) {
            SimpleFeatureCollection inPointsFC = getVector(inFile);

            fClusterName = FeatureUtilities.findAttributeName(inPointsFC.getSchema(), fClusterName);
            if (fClusterName == null) {
                throw new ModelsIllegalargumentException("No field found by the name: " + fClusterName, this);
            }
            CoordinateReferenceSystem crs = inPointsFC.getBounds().getCoordinateReferenceSystem();
            List<SimpleFeature> pointsList = FeatureUtilities.featureCollectionToList(inPointsFC);

            GvmClusters<GvmVectorSpace, Coordinate> clusters = new GvmClusters<GvmVectorSpace, Coordinate>(space, pClusterCount);
            clusters.setKeyer(new ShpClusterElevationKeyer());
            double[] vector = clusters.getSpace().newOrigin();
            pm.beginTask("Clustering...", pointsList.size());
            for( SimpleFeature point : pointsList ) {
                Geometry geometry = (Geometry) point.getDefaultGeometry();
                Coordinate dot = geometry.getCoordinate();
                vector[0] = dot.x;
                vector[1] = dot.y;
                double elev = (Double) point.getAttribute(fClusterName);
                dot.z = elev;
                clusters.add(elev, vector, dot);

                pm.worked(1);
            }
            pm.done();

            SimpleFeatureBuilder builder = getfeatureBuilder(crs);
            double delta = 0.0001;
            List<GvmResult<Coordinate>> results = clusters.results();
            for( GvmResult<Coordinate> gvmResult : results ) {
                int count = gvmResult.getCount();
                double mass = gvmResult.getMass();
                double variance = gvmResult.getVariance();
                double stdev = gvmResult.getStdDeviation();

                if (variance < delta || stdev < delta) {
                    continue;
                }

                double[] point = (double[]) gvmResult.getPoint();
                double avgElev = mass / count;

                Point p = gf.createPoint(new Coordinate(point[0], point[1]));
                Object[] values = new Object[]{p, count, mass, variance, stdev, avgElev};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                newCollection.add(feature);
            }

        } else {
            throw new ModelsIllegalargumentException("Can't process input file: " + inFile, this);
        }

        OmsVectorWriter.writeVector(outShp, newCollection);
    }

    private SimpleFeatureBuilder getfeatureBuilder( CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("cluster");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("count", Integer.class);
        b.add("mass", Double.class);
        b.add("variance", Double.class);
        b.add("stdev", Double.class);
        b.add("elev", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        return builder;
    }

}