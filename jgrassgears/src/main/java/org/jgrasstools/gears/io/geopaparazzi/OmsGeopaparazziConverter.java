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
package org.jgrasstools.gears.io.geopaparazzi;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

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

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

@Description("Coverts a geopaparazzi project into shapefiles.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("geopaparazzi, vector")
@Label(JGTConstants.VECTORPROCESSING)
@Name("geopapconvert")
@Status(Status.DRAFT)
@License("General Public License Version 3 (GPLv3)")
public class OmsGeopaparazziConverter extends JGTModel {

    @Description("The geopaparazzi folder")
    @UI(JGTConstants.FOLDERIN_UI_HINT)
    @In
    public String inGeopaparazzi = null;

    @Description("The output folder")
    @UI(JGTConstants.FOLDEROUT_UI_HINT)
    @In
    public String outData = null;

    private final DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
    private static boolean hasDriver = false;

    static {
        try {
            // make sure sqlite driver are there
            Class.forName("org.sqlite.JDBC");
            hasDriver = true;
        } catch (Exception e) {
        }
    }

    @Execute
    public void process() throws IOException {
        checkNull(inGeopaparazzi);

        if (!hasDriver) {
            throw new ModelsIllegalargumentException("Can't find any sqlite driver. Check your settings.", this);
        }

        File geopapFolderFile = new File(inGeopaparazzi);
        File geopapDatabaseFile = new File(geopapFolderFile, "geopaparazzi.db");

        if (!geopapDatabaseFile.exists()) {
            throw new ModelsIllegalargumentException(
                    "The geopaparazzi database file (geopaparazzi.db) is missing. Check the inserted path.", this);
        }

        File outputFolderFile = new File(outData);

        Connection connection;
        try {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + geopapDatabaseFile.getAbsolutePath());
            if (geopapDatabaseFile.exists()) {
                /*
                 * import notes as shapefile
                 */
                notesToShapefile(connection, outputFolderFile, pm);

                /*
                 * import gps logs as shapefiles, once as lines and once as points
                 */
                gpsLogToShapefiles(connection, outputFolderFile, pm);
            }
            /*
             * import media as point shapefile, containin gthe path
             */
            mediaToShapeFile(geopapFolderFile, outputFolderFile, pm);

        } catch (Exception e) {
            throw new ModelsRuntimeException("An error occurred while importing from geopaparazzi.", this);
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                // connection close failed.
                throw new ModelsRuntimeException("An error occurred while closing the database connection.", this);
            }
        }

    }

    private void notesToShapefile( Connection connection, File outputFolderFile, IJGTProgressMonitor pm ) throws Exception {
        File outputShapeFile = new File(outputFolderFile, "notes.shp");

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("geopaparazzisimlenotes"); //$NON-NLS-1$
        b.setCRS(crs);
        b.add("the_geom", Point.class); //$NON-NLS-1$
        b.add("DESCRIPTION", String.class);
        b.add("TIMESTAMP", String.class);
        b.add("ALTIM", Double.class);

        SimpleFeatureType featureType = b.buildFeatureType();
        pm.beginTask("Import notes...", -1);
        SimpleFeatureCollection newCollection = FeatureCollections.newCollection();

        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery("select lat, lon, altim, ts, text, form from notes");
            while( rs.next() ) {
                String form = rs.getString("form");
                if (form != null || form.trim().length() > 0) {
                    continue;
                }

                double lat = rs.getDouble("lat");
                double lon = rs.getDouble("lon");
                double altim = rs.getDouble("altim");
                String dateTimeString = rs.getString("ts");
                String text = rs.getString("text");

                if (lat == 0 || lon == 0) {
                    continue;
                }

                // and then create the features
                Coordinate c = new Coordinate(lon, lat);
                Point point = gf.createPoint(c);

                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
                Object[] values = new Object[]{point, text, dateTimeString, String.valueOf(altim)};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                newCollection.add(feature);
                pm.worked(1);
            }

            OmsVectorWriter.writeVector(outputShapeFile.getAbsolutePath(), newCollection);
        } finally {
            pm.done();
            if (statement != null)
                statement.close();
        }

    }

}
