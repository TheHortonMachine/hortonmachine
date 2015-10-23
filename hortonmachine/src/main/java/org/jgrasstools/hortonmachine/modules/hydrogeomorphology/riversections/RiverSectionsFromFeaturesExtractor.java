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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.coverage.ProfilePoint;
import org.jgrasstools.gears.utils.features.FeatureMate;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

/**
 * An extractor of geometries from dtm through features for Hecras.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public class RiverSectionsFromFeaturesExtractor extends ARiverSectionsExtractor {

    /**
     * Constructor.
     * 
     * @param riverLine the river line to consider for the cross sections extraction.
     * @param elevation the elevation {@link GridCoverage2D}.
     * @param sectionsList the list of sections to use.
     * @param monitor the progress monitor.
     * @throws Exception
     */
    public RiverSectionsFromFeaturesExtractor( //
            LineString riverLine, //
            GridCoverage2D elevation, //
            List<FeatureMate> sectionsList, //
            IJGTProgressMonitor monitor //
    ) throws Exception {
        crs = elevation.getCoordinateReferenceSystem();
        RandomIter elevIter = CoverageUtilities.getRandomIterator(elevation);
        GridGeometry2D gridGeometry = elevation.getGridGeometry();
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(elevation);
        Envelope envelope = regionMap.toEnvelope();

        monitor.beginTask("Extracting sections...", sectionsList.size());

        riverPointsList = new ArrayList<RiverPoint>();
        LengthIndexedLine indexedLine = new LengthIndexedLine(riverLine);
        for( FeatureMate sectionMate : sectionsList ) {
            Coordinate[] coordinates = sectionMate.getGeometry().getCoordinates();

            List<ProfilePoint> profilePoints = CoverageUtilities.doProfile(elevIter, gridGeometry,
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
            RiverPoint netPoint = new RiverPoint(crossPointCoordinate, crossPointIndex, line3d, null);
            if (netPoint != null)
                riverPointsList.add(netPoint);
            monitor.worked(1);
        }
        monitor.done();

        // add also the river coordinates that do not have sections
        Coordinate[] coordinates = riverLine.getCoordinates();
        List<ProfilePoint> riverProfile = CoverageUtilities.doProfile(elevIter, gridGeometry, coordinates);
        for( ProfilePoint profilePoint : riverProfile ) {
            Coordinate position = profilePoint.getPosition();
            if (envelope.intersects(position)) {
                position.z = profilePoint.getElevation();
                RiverPoint netPoint = new RiverPoint(position, profilePoint.getProgressive(), null, null);
                riverPointsList.add(netPoint);
            }
        }

        pointsWithSectionsNum = 0;
        for( RiverPoint netPoint : riverPointsList ) {
            if (netPoint.hasSection) {
                pointsWithSectionsNum++;
            }
        }
        Collections.sort(riverPointsList);

    }

}
