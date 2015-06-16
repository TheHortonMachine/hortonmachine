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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.riversections;

import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Abstract class for river extractors.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public abstract class ARiverSectionsExtractor {

    public static final String FIELD_SECTIONPOINT_ELEV = "elev";
    public static final String FIELD_ELEVATION = "elev";
    public static final String FIELD_SECTIONPOINT_INDEX = "index";
    public static final String FIELD_SECTION_ID = "sectionid";
    public static final String FIELD_PROGRESSIVE = "prog";

    protected int pointsWithSectionsNum;
    protected GeometryFactory gf = GeometryUtilities.gf();
    protected List<RiverPoint> riverPointsList;
    protected CoordinateReferenceSystem crs;
    protected SimpleFeatureCollection sectionsCollection;
    protected SimpleFeatureCollection sectionPointsCollection;
    protected SimpleFeatureCollection riverPointsCollection;

    public List<RiverPoint> getOrderedNetworkPoints() {
        return riverPointsList;
    }

    public int getSectionsNum() {
        return pointsWithSectionsNum;
    }

    public SimpleFeatureCollection getSectionsCollection() {
        if (sectionsCollection == null)
            createSectionsCollections();
        return sectionsCollection;
    }

    public SimpleFeatureCollection getSectionPointsCollection() {
        if (sectionPointsCollection == null)
            createSectionsCollections();
        return sectionPointsCollection;
    }

    public SimpleFeatureCollection getRiverPointsCollection() {
        if (riverPointsCollection == null)
            createSectionsCollections();
        return riverPointsCollection;
    }

    protected void createSectionsCollections() {
        SimpleFeatureTypeBuilder sectionTypeBuilder = new SimpleFeatureTypeBuilder();
        sectionTypeBuilder.setName("sectionlines");
        sectionTypeBuilder.setCRS(crs);
        sectionTypeBuilder.add("the_geom", LineString.class);
        sectionTypeBuilder.add(FIELD_SECTION_ID, Integer.class);
        sectionTypeBuilder.add(FIELD_PROGRESSIVE, Double.class);
        SimpleFeatureType sectionType = sectionTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder sectionBuilder = new SimpleFeatureBuilder(sectionType);

        SimpleFeatureTypeBuilder riverPointsTypeBuilder = new SimpleFeatureTypeBuilder();
        riverPointsTypeBuilder.setName("riverpoints");
        riverPointsTypeBuilder.setCRS(crs);
        riverPointsTypeBuilder.add("the_geom", Point.class);
        riverPointsTypeBuilder.add(FIELD_SECTION_ID, Integer.class);
        riverPointsTypeBuilder.add(FIELD_PROGRESSIVE, Double.class);
        riverPointsTypeBuilder.add(FIELD_ELEVATION, Double.class);
        SimpleFeatureType riverType = riverPointsTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder riverBuilder = new SimpleFeatureBuilder(riverType);

        SimpleFeatureTypeBuilder sectionPointsTypeBuilder = new SimpleFeatureTypeBuilder();
        sectionPointsTypeBuilder.setName("sectionpoint");
        sectionPointsTypeBuilder.setCRS(crs);
        sectionPointsTypeBuilder.add("the_geom", Point.class);
        sectionPointsTypeBuilder.add(FIELD_SECTIONPOINT_INDEX, Integer.class);
        sectionPointsTypeBuilder.add(FIELD_SECTIONPOINT_ELEV, Double.class);
        sectionPointsTypeBuilder.add(FIELD_PROGRESSIVE, Double.class);
        sectionPointsTypeBuilder.add(FIELD_SECTION_ID, Integer.class);
        SimpleFeatureType sectionPointsType = sectionPointsTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder sectionPointsBuilder = new SimpleFeatureBuilder(sectionPointsType);

        sectionsCollection = new DefaultFeatureCollection();
        sectionPointsCollection = new DefaultFeatureCollection();
        riverPointsCollection = new DefaultFeatureCollection();

        int index = 0;
        for( RiverPoint netPoint : riverPointsList ) {
            int sectionId = netPoint.getSectionId();
            if (sectionId == -1) {
                sectionId = index;
            }
            if (netPoint.hasSection) {
                LineString sectionGeometry = netPoint.sectionGeometry;

                Coordinate[] sectionCoordinates = sectionGeometry.getCoordinates();
                LineString simpleSectionGeometry = gf.createLineString(new Coordinate[]{sectionCoordinates[0],
                        sectionCoordinates[sectionCoordinates.length - 1]});

                Object[] sectionValues = new Object[]{simpleSectionGeometry, sectionId, netPoint.progressiveDistance};
                sectionBuilder.addAll(sectionValues);

                SimpleFeature sectionFeature = sectionBuilder.buildFeature(null);
                ((DefaultFeatureCollection) sectionsCollection).add(sectionFeature);

                Coordinate[] coordinates = sectionGeometry.getCoordinates();
                List<Double> sectionProgressive = netPoint.sectionProgressive;
                for( int i = 0; i < coordinates.length; i++ ) {
                    Point point = gf.createPoint(coordinates[i]);

                    Object[] sectionPointsValues = new Object[]{point, i, coordinates[i].z, sectionProgressive.get(i), sectionId};
                    sectionPointsBuilder.addAll(sectionPointsValues);
                    SimpleFeature sectionPointsFeature = sectionPointsBuilder.buildFeature(null);
                    ((DefaultFeatureCollection) sectionPointsCollection).add(sectionPointsFeature);
                }
            } else {
                // if there is no section, a single point is placed
                double progressiveDistance = netPoint.progressiveDistance;
                Coordinate coord = netPoint.point;
                Point point = gf.createPoint(coord);

                Object[] riverPointValues = new Object[]{point, sectionId, progressiveDistance, coord.z};
                riverBuilder.addAll(riverPointValues);
                SimpleFeature riverFeature = riverBuilder.buildFeature(null);
                ((DefaultFeatureCollection) riverPointsCollection).add(riverFeature);
            }
            index++;
        }
    }
}
