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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.hecras;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.coverage.ProfilePoint;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

/**
 * An extractor of geometries from dtm through features for Hecras.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HecrasSectionsFromFeaturesExtractor implements HecrasSectionsExtractor {

    private GeometryFactory gf = GeometryUtilities.gf();
    private List<NetworkPoint> networkPointList;
    private int pointsWithSectionsNum;
    private CoordinateReferenceSystem crs;
    private SimpleFeatureCollection sectionsCollection;
    private SimpleFeatureCollection sectionPointsCollection;

    /**
     * Constructor.
     * 
     * @param riverLine the river line to consider for the cross sections extraction.
     * @param elevation the elevation {@link GridCoverage2D}.
     * @param sectionsList the list of sections to use.
     * @param monitor the progress monitor.
     * @throws Exception
     */
    public HecrasSectionsFromFeaturesExtractor( //
            LineString riverLine, //
            GridCoverage2D elevation, //
            List<FeatureMate> sectionsList, //
            IJGTProgressMonitor monitor //
    ) throws Exception {
        crs = elevation.getCoordinateReferenceSystem();
        RenderedImage elevRI = elevation.getRenderedImage();
        RandomIter elevIter = RandomIterFactory.create(elevRI, null);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(elevation);
        GridGeometry2D gridGeometry = elevation.getGridGeometry();

        monitor.beginTask("Extracting sections...", -1);

        networkPointList = new ArrayList<NetworkPoint>();
        LengthIndexedLine indexedLine = new LengthIndexedLine(riverLine);
        for( FeatureMate sectionMate : sectionsList ) {
            Coordinate[] coordinates = sectionMate.getGeometry().getCoordinates();

            List<ProfilePoint> profilePoints = CoverageUtilities.doProfile(elevIter, regionMap, gridGeometry,
                    coordinates[coordinates.length - 1], coordinates[0]);
            List<Coordinate> coordinate3dList = new ArrayList<Coordinate>();
            for( ProfilePoint profilePoint : profilePoints ) {
                Coordinate position = profilePoint.getPosition();
                position.z = profilePoint.getElevation();
                coordinate3dList.add(position);
            }
            LineString line3d = gf.createLineString(coordinate3dList.toArray(new Coordinate[0]));
            Geometry crossPoint = line3d.intersection(riverLine);
            Coordinate crossPointCoordinate = crossPoint.getCoordinate();

            double crossPointIndex = indexedLine.indexOf(crossPointCoordinate);

            int[] colRow = CoverageUtilities.colRowFromCoordinate(crossPointCoordinate, gridGeometry, null);
            double elev = elevIter.getSampleDouble(colRow[0], colRow[1], 0);
            crossPointCoordinate.z = elev;
            NetworkPoint netPoint = new NetworkPoint(crossPointCoordinate, crossPointIndex, line3d);
            if (netPoint != null)
                networkPointList.add(netPoint);
        }

        // add also the river coordinates that do not have sections
        Coordinate[] coordinates = riverLine.getCoordinates();
        List<ProfilePoint> riverPprofile = CoverageUtilities.doProfile(elevIter, regionMap, gridGeometry, coordinates);
        for( ProfilePoint profilePoint : riverPprofile ) {
            Coordinate position = profilePoint.getPosition();
            position.z = profilePoint.getElevation();
            NetworkPoint netPoint = new NetworkPoint(position, profilePoint.getProgressive(), null);
            networkPointList.add(netPoint);
        }

        pointsWithSectionsNum = 0;
        for( NetworkPoint netPoint : networkPointList ) {
            if (netPoint.hasSection) {
                pointsWithSectionsNum++;
            }
        }
        Collections.sort(networkPointList);

        monitor.done();

    }

    private void createSectionsCollections() {
        SimpleFeatureTypeBuilder sectionTypeBuilder = new SimpleFeatureTypeBuilder();
        sectionTypeBuilder.setName("sectionlines");
        sectionTypeBuilder.setCRS(crs);
        sectionTypeBuilder.add("the_geom", LineString.class);
        sectionTypeBuilder.add("id", Integer.class);
        sectionTypeBuilder.add("progressive", Double.class);
        SimpleFeatureType sectionType = sectionTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder sectionBuilder = new SimpleFeatureBuilder(sectionType);

        SimpleFeatureTypeBuilder sectionPointsTypeBuilder = new SimpleFeatureTypeBuilder();
        sectionPointsTypeBuilder.setName("sectionpoint");
        sectionPointsTypeBuilder.setCRS(crs);
        sectionPointsTypeBuilder.add("the_geom", Point.class);
        sectionPointsTypeBuilder.add("count", Integer.class);
        sectionPointsTypeBuilder.add("elev", Double.class);
        sectionPointsTypeBuilder.add("progressive", Double.class);
        sectionPointsTypeBuilder.add("sectionid", Double.class);
        SimpleFeatureType sectionPointsType = sectionPointsTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder sectionPointsBuilder = new SimpleFeatureBuilder(sectionPointsType);

        sectionsCollection = FeatureCollections.newCollection();
        sectionPointsCollection = FeatureCollections.newCollection();

        int index = 0;
        for( NetworkPoint netPoint : networkPointList ) {
            if (!netPoint.hasSection) {
                continue;
            }
            int sectionId = netPoint.getSectionId();
            if (sectionId == -1) {
                sectionId = index;
            }
            LineString sectionGeometry = netPoint.sectionGeometry;
            Object[] sectionValues = new Object[]{sectionGeometry, sectionId, netPoint.progressiveDistance};
            sectionBuilder.addAll(sectionValues);
            SimpleFeature sectionFeature = sectionBuilder.buildFeature(null);
            sectionsCollection.add(sectionFeature);

            Coordinate[] coordinates = sectionGeometry.getCoordinates();
            List<Double> sectionProgressive = netPoint.sectionProgressive;
            for( int i = 0; i < coordinates.length; i++ ) {
                Point point = gf.createPoint(coordinates[i]);

                Object[] sectionPointsValues = new Object[]{point, i, coordinates[i].z, sectionProgressive.get(i), sectionId};
                sectionPointsBuilder.addAll(sectionPointsValues);
                SimpleFeature sectionPointsFeature = sectionPointsBuilder.buildFeature(null);
                sectionPointsCollection.add(sectionPointsFeature);
            }
            index++;
        }
    }

    @Override
    public List<NetworkPoint> getOrderedNetworkPoints() {
        return networkPointList;
    }

    @Override
    public int getSectionsNum() {
        return pointsWithSectionsNum;
    }

    @Override
    public SimpleFeatureCollection getSectionsCollection() {
        if (sectionsCollection == null)
            createSectionsCollections();
        return sectionsCollection;
    }

    @Override
    public SimpleFeatureCollection getSectionPointsCollection() {
        if (sectionPointsCollection == null)
            createSectionsCollections();
        return sectionPointsCollection;
    }
}
