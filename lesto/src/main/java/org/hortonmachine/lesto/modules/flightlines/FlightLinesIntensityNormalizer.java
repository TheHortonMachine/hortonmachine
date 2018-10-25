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
package org.hortonmachine.lesto.modules.flightlines;
import static java.lang.Math.floor;
import static java.lang.Math.pow;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.GpsTimeConverter;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.index.strtree.STRtree;

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

@Description("A module that normalizes flightlines based on the aircraft position.")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("las, normalize, flightlines")
@Label(HMConstants.LESTO + "/flightlines")
@Name("lasflightlinesnormalization")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class FlightLinesIntensityNormalizer extends HMModel {
    @Description("A las file.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inLas;

    @Description("Shapefile containing the string fields of date, time, elev.")
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inFlightpoints;

    @Description("The square of standard range.")
    @In
    public double pStdRange = 600.0;

    @Description("The date and time pattern (date and time field content will be concatenated through a space).")
    @In
    public String pDateTimePattern = "yyyy-MM-dd HH:mm:ss";

    @Description("The gps time type used in the las file.")
    @UI("combo:" + FlightLinesExtractor.ADJUSTED_STANDARD_GPS_TIME + "," + FlightLinesExtractor.WEEK_SECONDS_TIME)
    @In
    public String pGpsTimeType = FlightLinesExtractor.ADJUSTED_STANDARD_GPS_TIME;

    @Description("Normalized output las file.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outLas;

    @Execute
    public void process() throws Exception {
        checkNull(inLas, inFlightpoints, pDateTimePattern);

        int timeType = -1;
        if (pGpsTimeType.equals(FlightLinesExtractor.ADJUSTED_STANDARD_GPS_TIME)) {
            timeType = 1;
        }
        if (pGpsTimeType.equals(FlightLinesExtractor.WEEK_SECONDS_TIME)) {
            timeType = 0;
        }

        SimpleFeatureCollection flightPointsFC = OmsVectorReader.readVector(inFlightpoints);
        List<SimpleFeature> flightPointsList = FeatureUtilities.featureCollectionToList(flightPointsFC);
        SimpleFeatureType schema = flightPointsFC.getSchema();

        String dateName = FeatureUtilities.findAttributeName(schema, "date");
        String timeName = FeatureUtilities.findAttributeName(schema, "time");
        String elevName = FeatureUtilities.findAttributeName(schema, "elev");
        if (dateName == null || timeName == null || elevName == null) {
            throw new ModelsIllegalargumentException("The shapefile has to contain the fields date time and elev.", this);
        }

        pm.beginTask("Defining flight intervals and positions...", flightPointsList.size());
        DateTimeFormatter formatter = DateTimeFormat.forPattern(pDateTimePattern).withZone(DateTimeZone.UTC);
        TreeMap<DateTime, Coordinate> date2pointsMap = new TreeMap<DateTime, Coordinate>();
        TreeMap<Coordinate, DateTime> points2dateMap = new TreeMap<Coordinate, DateTime>();
        for( int i = 0; i < flightPointsList.size(); i++ ) {
            SimpleFeature flightPoint = flightPointsList.get(i);
            Geometry g1 = (Geometry) flightPoint.getDefaultGeometry();
            Coordinate c1 = g1.getCoordinate();
            double elev1 = ((Number) flightPoint.getAttribute(elevName)).doubleValue();
            c1.z = elev1;
            String date1 = flightPoint.getAttribute(dateName).toString();
            String time1 = flightPoint.getAttribute(timeName).toString();
            String dateTime1 = date1 + " " + time1;
            DateTime d1 = formatter.parseDateTime(dateTime1);
            date2pointsMap.put(d1, c1);
            points2dateMap.put(c1, d1);
            pm.worked(1);
        }
        pm.done();

        pm.beginTask("Create time index...", flightPointsList.size() - 1);
        DateTime minDate = null;
        DateTime maxDate = null;
        long minLong = Long.MAX_VALUE;
        long maxLong = -Long.MAX_VALUE;
        STRtree tree = new STRtree(flightPointsList.size());
        Set<Entry<DateTime, Coordinate>> pointsSet = date2pointsMap.entrySet();
        Entry[] array = pointsSet.toArray(new Entry[0]);
        for( int i = 0; i < array.length - 1; i++ ) {
            DateTime d1 = (DateTime) array[i].getKey();
            Coordinate c1 = (Coordinate) array[i].getValue();
            DateTime d2 = (DateTime) array[i + 1].getKey();
            Coordinate c2 = (Coordinate) array[i + 1].getValue();
            long millis1 = d1.getMillis();
            long millis2 = d2.getMillis();
            Envelope timeEnv = new Envelope(millis1, millis2, millis1, millis2);
            tree.insert(timeEnv, new Coordinate[]{c1, c2});
            if (millis1 < minLong) {
                minLong = millis1;
                minDate = d1;
            }
            if (millis2 > maxLong) {
                maxLong = millis2;
                maxDate = d2;
            }
            pm.worked(1);
        }
        pm.done();

        StringBuilder sb = new StringBuilder();
        sb.append("Flight data interval: ");
        sb.append(minDate.toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS));
        sb.append(" to ");
        sb.append(maxDate.toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS));
        pm.message(sb.toString());

        CoordinateReferenceSystem crs = null;
        File lasFile = new File(inLas);
        File outLasFile = new File(outLas);
        try (ALasReader reader = ALasReader.getReader(lasFile, crs); //
                ALasWriter writer = ALasWriter.getWriter(outLasFile, crs);) {
            reader.setOverrideGpsTimeType(timeType);
            ILasHeader header = reader.getHeader();
            int gpsTimeType = header.getGpsTimeType();
            writer.setBounds(header);
            writer.open();

            pm.beginTask("Interpolating flight points and normalizing...", (int) header.getRecordsCount());
            while( reader.hasNextPoint() ) {
                LasRecord r = reader.getNextPoint();

                DateTime gpsTimeToDateTime;
                if (timeType == 0) {
                    gpsTimeToDateTime = GpsTimeConverter.gpsWeekTime2DateTime(r.gpsTime);
                } else {
                    gpsTimeToDateTime = LasUtils.adjustedStandardGpsTime2DateTime(r.gpsTime);
                }
                long gpsMillis = gpsTimeToDateTime.getMillis();
                Coordinate lasCoordinate = new Coordinate(r.x, r.y, r.z);
                Envelope pEnv = new Envelope(new Coordinate(gpsMillis, gpsMillis));
                List points = tree.query(pEnv);
                Coordinate[] flightCoords = (Coordinate[]) points.get(0);
                long d1 = points2dateMap.get(flightCoords[0]).getMillis();
                long d2 = points2dateMap.get(flightCoords[1]).getMillis();

                LineSegment line = new LineSegment(flightCoords[0], flightCoords[1]);
                double fraction = (gpsMillis - d1) / (d2 - d1);
                Coordinate interpolatedFlightPoint = line.pointAlong(fraction);
                // calculate interpolated elevation

                double distX = interpolatedFlightPoint.distance(flightCoords[0]);
                double dist12 = flightCoords[1].distance(flightCoords[0]);
                double interpolatedElev = distX / dist12 * (flightCoords[1].z - flightCoords[0].z) + flightCoords[0].z;
                interpolatedFlightPoint.z = interpolatedElev;

                double distanceFlightTerrain = GeometryUtilities.distance3d(lasCoordinate, interpolatedFlightPoint, null);

                short norm = (short) floor(r.intensity * pow(distanceFlightTerrain, 2.0) / pStdRange + 0.5);
                r.intensity = norm;
                writer.addPoint(r);

                pm.worked(1);
            }
            pm.done();
        }
    }
}
