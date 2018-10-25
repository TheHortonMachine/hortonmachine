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
package org.hortonmachine.nww.utils;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.layers.defaults.other.CurrentGpsPointLayer;
import org.hortonmachine.nww.layers.defaults.other.SimplePointsLayer;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;

public class FakeGps {

    public FakeGps(File shapefile, NwwPanel nwwPanel, CurrentGpsPointLayer gpsPointLayer,
            SimplePointsLayer simplePointsLayer) throws Exception {

        SimpleFeatureCollection fc = NwwUtilities.readAndReproject(shapefile.getAbsolutePath());
        List<Geometry> geomsList = FeatureUtilities.featureCollectionToGeometriesList(fc, true, null);

        List<Coordinate> coordsList = new ArrayList<>();
        for (Geometry geometry : geomsList) {
            Coordinate[] coordinates = geometry.getCoordinates();
            for (Coordinate coordinate : coordinates) {
                coordsList.add(coordinate);
            }
        }

        final WorldWindow wwd = nwwPanel.getWwd();
        LineString lineString = GeometryUtilities.gf().createLineString(coordsList.toArray(new Coordinate[0]));

        Coordinate sampleCoordinate = geomsList.get(0).getCoordinate();
        GeodeticCalculator gc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
        gc.setStartingGeographicPoint(sampleCoordinate.x, sampleCoordinate.y);
        gc.setDirection(90, 2);
        Point2D destinationGeographicPoint = gc.getDestinationGeographicPoint();
        Coordinate dest = new Coordinate(destinationGeographicPoint.getX(), destinationGeographicPoint.getY());

        double distance = sampleCoordinate.distance(dest);

        List<Coordinate> dataList = GeometryUtilities.getCoordinatesAtInterval(lineString, distance, false, -1, -1);

        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);

                    View view = wwd.getView();
                    Angle heading = view.getHeading();

                    for (int i = 1; i < dataList.size(); i++) {
                        Coordinate c1 = dataList.get(i - 1);
                        Coordinate c2 = dataList.get(i);

                        double azimuth = GeometryUtilities.azimuth(c1, c2);

                        double y = c2.y;
                        double x = c2.x;
                        simplePointsLayer.addNewPoint(y, x);
                        gpsPointLayer.updatePosition(y, x);
                        LatLon latLon = new LatLon(Angle.fromDegrees(y), Angle.fromDegrees(x));
                        heading = Angle.fromDegrees(azimuth);

                        double altitude = wwd.getView().getCurrentEyePosition().getAltitude();

                        view.setEyePosition(new Position(latLon, altitude));
                        view.setHeading(heading);
                        nwwPanel.getWwd().redraw();
                        if (i == 0) {
                            Thread.sleep(2000);
                        } else {
                            Thread.sleep(100);
                        }
                    }

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }).start();

    }

}
