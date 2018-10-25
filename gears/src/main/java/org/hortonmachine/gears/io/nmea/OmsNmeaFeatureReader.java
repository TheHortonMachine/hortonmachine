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
package org.hortonmachine.gears.io.nmea;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.time.UtcTimeUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

@Description(OmsNmeaFeatureReader.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsNmeaFeatureReader.KEYWORDS)
@Label(HMConstants.FEATUREREADER)
@Name(OmsNmeaFeatureReader.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsNmeaFeatureReader extends HMModel {

    public static final String geodata_DESCRIPTION = "The read feature collection.";
    public static final String file_DESCRIPTION = "The NMEA input file.";
    public static final String NAME = "nmeafeaturereader";
    public static final String KEYWORDS = "NMEA, gps, vector";
    public static final String DESCRIPTION = "A reader for NMEA gps sentences.";

    @Description(file_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String file = null;

    @Description(geodata_DESCRIPTION)
    @Out
    public SimpleFeatureCollection geodata = null;

    @Execute
    public void readFeatureCollection() throws IOException {
        checkNull(file);
        checkFileExists(file);

        List<String> linesList = FileUtilities.readFileToLinesList(new File(file));
        Iterator<String> linesIterator = linesList.iterator();
        while( linesIterator.hasNext() ) {
            String line = linesIterator.next();
            if (!line.startsWith(NmeaGpsPoint.GPGGA) && !line.startsWith(NmeaGpsPoint.GPRMC)) {
                linesIterator.remove();
            }
        }

        DefaultFeatureCollection newFC = new DefaultFeatureCollection();
        SimpleFeatureBuilder builder = getNmeaFeatureBuilder();

        String gpgga = null;
        String gprmc = null;
        for( String line : linesList ) {
            if (line.startsWith(NmeaGpsPoint.GPGGA) && gpgga == null) {
                gpgga = line;
            }
            if (line.startsWith(NmeaGpsPoint.GPRMC) && gprmc == null) {
                gprmc = line;
            }
            if (gpgga != null && gprmc != null) {
                NmeaGpsPoint point = new NmeaGpsPoint(gpgga, gprmc);
                if (point.isValid) {
                    Point p = gf.createPoint(new Coordinate(point.longitude, point.latitude));
                    Object[] attributes = new Object[]{p, //
                            point.speed,//
                            point.altitude,//
                            point.quality,//
                            (int) point.sat,//
                            point.hdop,//
                            point.ellipsoidVsMsl,//
                            UtcTimeUtilities.toStringWithSeconds(point.utcDateTime),//
                            point.mag_var,//
                            point.angle//
                    };

                    builder.addAll(attributes);
                    SimpleFeature nmeaFeature = builder.buildFeature(null);
                    newFC.add(nmeaFeature);
                }
                gpgga = null;
                gprmc = null;
            }

        }

        geodata = newFC;
    }

    public static SimpleFeatureBuilder getNmeaFeatureBuilder() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("nmea");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        b.add(NmeaGpsPoint.strSpeed, Double.class);
        b.add(NmeaGpsPoint.strAltitude, Double.class);
        b.add(NmeaGpsPoint.strQuality, Double.class);
        b.add(NmeaGpsPoint.strSat, Integer.class);
        b.add(NmeaGpsPoint.strHdop, Double.class);
        b.add(NmeaGpsPoint.strMsl, Double.class);
        b.add(NmeaGpsPoint.strUtctime, String.class);
        b.add(NmeaGpsPoint.strMag_var, Double.class);
        b.add(NmeaGpsPoint.strAngle, Double.class);
        final SimpleFeatureType featureType = b.buildFeatureType();
        SimpleFeatureBuilder nmeaSimpleFeatureBuilder = new SimpleFeatureBuilder(featureType);
        return nmeaSimpleFeatureBuilder;
    }

    /**
     * Fast read access mode. 
     * 
     * @param path the NMEA file path.
     * @return the read {@link FeatureCollection}.
     * @throws IOException
     */
    public static SimpleFeatureCollection readNMEAfile( String path ) throws IOException {

        OmsNmeaFeatureReader reader = new OmsNmeaFeatureReader();
        reader.file = path;
        reader.readFeatureCollection();

        return reader.geodata;
    }

}
