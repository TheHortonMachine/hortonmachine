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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment.LWFields;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

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
        sectionTypeBuilder.add(LWFields.GAUKLER, Double.class);
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

        int index = riverPointsList.size();
        for( RiverPoint netPoint : riverPointsList ) {
            int sectionId = netPoint.getSectionId();
            if (sectionId == -1) {
                sectionId = index--;
            }
            if (netPoint.hasSection) {
                LineString sectionGeometry = netPoint.sectionGeometry;

                Coordinate[] sectionCoordinates = sectionGeometry.getCoordinates();
                CoordinateArrays.reverse(sectionCoordinates);
                
                LineString simpleSectionGeometry = gf.createLineString(
                        new Coordinate[]{sectionCoordinates[0], sectionCoordinates[sectionCoordinates.length - 1]});

                Object[] sectionValues = new Object[]{simpleSectionGeometry, sectionId, netPoint.progressiveDistance,
                        netPoint.getSectionGauklerStrickler()};
                sectionBuilder.addAll(sectionValues);

                SimpleFeature sectionFeature = sectionBuilder.buildFeature(null);
                ((DefaultFeatureCollection) sectionsCollection).add(sectionFeature);

                Coordinate[] coordinates = sectionGeometry.getCoordinates();
                List<Double> sectionProgressive = netPoint.getSectionProgressive();
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
        }
    }

    /**
     * Get the river info as produced by the {@link OmsRiverSectionsExtractor}.
     * 
     * @param riverPointsFeatures the river points. 
     * @param sectionFeatures the extracted sections.
     * @param sectionPointsFeatures the section points.
     * @return the {@link RiverInfo}.
     */
    public static RiverInfo getRiverInfo( List<SimpleFeature> riverPointsFeatures, List<SimpleFeature> sectionFeatures,
            List<SimpleFeature> sectionPointsFeatures ) {
        RiverInfo riverInfo = new RiverInfo();

        for( SimpleFeature riverPointFeature : riverPointsFeatures ) {
            int sectionId = ((Number) riverPointFeature.getAttribute(ARiverSectionsExtractor.FIELD_SECTION_ID)).intValue();
            riverInfo.orderedRiverPoints.put(sectionId, riverPointFeature);
        }
        int count = 0;
        riverInfo.riverCoords = new Coordinate[riverInfo.orderedRiverPoints.size()];
        for( Entry<Integer, SimpleFeature> riverEntry : riverInfo.orderedRiverPoints.entrySet() ) {
            SimpleFeature riverPoint = riverEntry.getValue();
            Coordinate coordinate = ((Geometry) riverPoint.getDefaultGeometry()).getCoordinate();
            double elev = ((Number) riverPoint.getAttribute(ARiverSectionsExtractor.FIELD_ELEVATION)).doubleValue();
            riverInfo.riverCoords[count++] = new Coordinate(coordinate.x, coordinate.y, elev);
        }
        GeometryFactory gf = new GeometryFactory();
        LineString riverGeometry = gf.createLineString(riverInfo.riverCoords);

        for( SimpleFeature sectionFeature : sectionFeatures ) {
            int sectionId = ((Number) sectionFeature.getAttribute(ARiverSectionsExtractor.FIELD_SECTION_ID)).intValue();
            riverInfo.orderedSections.put(sectionId, sectionFeature);
        }

        HashMap<Integer, TreeMap<Integer, SimpleFeature>> sectionId2PointId2PointMap = new HashMap<>();
        for( SimpleFeature pointFeature : sectionPointsFeatures ) {
            int sectionId = ((Number) pointFeature.getAttribute(ARiverSectionsExtractor.FIELD_SECTION_ID)).intValue();
            TreeMap<Integer, SimpleFeature> pointId2PointMap = sectionId2PointId2PointMap.get(sectionId);
            if (pointId2PointMap == null) {
                pointId2PointMap = new TreeMap<>();
                sectionId2PointId2PointMap.put(sectionId, pointId2PointMap);
            }
            int pointId = ((Number) pointFeature.getAttribute(ARiverSectionsExtractor.FIELD_SECTIONPOINT_INDEX)).intValue();
            pointId2PointMap.put(pointId, pointFeature);
        }

        for( Entry<Integer, SimpleFeature> sectionEntry : riverInfo.orderedSections.entrySet() ) {
            Integer sectionId = sectionEntry.getKey();
            SimpleFeature sectionFeature = sectionEntry.getValue();
            double progressive = ((Number) sectionFeature.getAttribute(ARiverSectionsExtractor.FIELD_PROGRESSIVE)).doubleValue();
            Object attribute = sectionFeature.getAttribute(LWFields.GAUKLER);
            if (attribute == null) {
                throw new ModelsRuntimeException("The input section data do not have the value of KS.",
                        "ARiverSectionsExtractor");
            }
            double ks = ((Number) attribute).doubleValue();

            Geometry sectionLine = (Geometry) sectionFeature.getDefaultGeometry();

            Geometry intersectionPoint = riverGeometry.intersection(sectionLine);
            if (intersectionPoint == null) {
                throw new ModelsRuntimeException("All sections have to intersect the river line.",
                        "ARiverSectionsExtractor#getRiverInfo");
            }

            TreeMap<Integer, SimpleFeature> sectionPoints = sectionId2PointId2PointMap.get(sectionId);
            Coordinate[] sectionCoords = new Coordinate[sectionPoints.size()];
            count = 0;
            for( Entry<Integer, SimpleFeature> pointEntry : sectionPoints.entrySet() ) {
                SimpleFeature sectionPoint = pointEntry.getValue();
                sectionCoords[count++] = ((Geometry) sectionPoint.getDefaultGeometry()).getCoordinate();
            }
            LineString sectionLineWithPoints = gf.createLineString(sectionCoords);
            RiverPoint rp = new RiverPoint(intersectionPoint.getCoordinate(), progressive, sectionLineWithPoints, ks);
            rp.setSectionId(sectionId);
            riverInfo.orderedNetworkPoints.add(rp);
        }
        riverInfo.extractedSectionsCount = riverInfo.orderedNetworkPoints.size();

        for( Entry<Integer, SimpleFeature> riverEntry : riverInfo.orderedRiverPoints.entrySet() ) {
            SimpleFeature riverPoint = riverEntry.getValue();
            Coordinate coordinate = ((Geometry) riverPoint.getDefaultGeometry()).getCoordinate();
            double progressive = ((Number) riverPoint.getAttribute(ARiverSectionsExtractor.FIELD_PROGRESSIVE)).doubleValue();
            RiverPoint rp = new RiverPoint(coordinate, progressive, null, null);
            rp.setSectionId(riverEntry.getKey());
            riverInfo.orderedNetworkPoints.add(rp);
        }

        Collections.sort(riverInfo.orderedNetworkPoints);
        return riverInfo;
    }

    public static List<RiverPoint> riverInfo2RiverPoints( RiverInfo riverInfo ) {
        List<RiverPoint> sectionPoints = new ArrayList<RiverPoint>();
        int orderedNetworkPointsSize = riverInfo.orderedNetworkPoints.size();
        for( int i = 0; i < orderedNetworkPointsSize; i++ ) {
            RiverPoint currentNetworkPoint = riverInfo.orderedNetworkPoints.get(i);
            if (currentNetworkPoint.hasSection) {
                sectionPoints.add(currentNetworkPoint);
            }
        }
        return sectionPoints;
    }
}
