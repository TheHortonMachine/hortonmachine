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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.io.FilenameFilter;

import org.geotools.styling.Style;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

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

@Description(GeopaparazziGeopackageCreator.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(GeopaparazziGeopackageCreator.OmsGeopaparazziGeopackageCreator_TAGS)
@Label(HMConstants.MOBILE)
@Name(GeopaparazziGeopackageCreator.OmsGeopaparazziGeopackageCreator_NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class GeopaparazziGeopackageCreator extends HMModel {

    @Description(THE_GEOPAPARAZZI_DATABASE_FILE)
    @UI(HMConstants.FILEIN_UI_HINT_GPAP)
    @In
    public String inGeopackage = null;

    @Description(OmsGeopaparazziGeopackageCreator_inShapefilesFolder)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inShapefilesFolder = null;

    // VARS DOCS START
    public static final String THE_GEOPAPARAZZI_DATABASE_FILE = "The existing or new geopackage database file.";
    public static final String DESCRIPTION = "Creates a geopackage database for geopaparazzi/smash from a set of shapefiles or adds to an existing one.";
    public static final String OmsGeopaparazziSpatialiteCreator_LABEL = HMConstants.VECTORPROCESSING;
    public static final String OmsGeopaparazziGeopackageCreator_TAGS = "geopaparazzi, vector";
    public static final String OmsGeopaparazziGeopackageCreator_NAME = "geopaparazzigeopackagecreator";
    public static final String OmsGeopaparazziGeopackageCreator_inShapefilesFolder = "The folder of shapefiles to import.";
    // VARS DOCS END

    @Execute
    public void process() throws Exception {
        checkNull(inGeopackage, inShapefilesFolder);

        File shpFolder = new File(inShapefilesFolder);
        File[] shpfiles = shpFolder.listFiles(new FilenameFilter(){
            @Override
            public boolean accept( File dir, String name ) {
                return name.endsWith(".shp");
            }
        });

        if (shpfiles.length == 0) {
            throw new ModelsIOException("The supplied folder doesn't contain any shapefile.", this);
        }

        try (ASpatialDb db = new GeopackageDb()) {
            if (!db.open(inGeopackage)) {
                db.initSpatialMetadata(null);
            }

            pm.beginTask("Importing shapefiles...", shpfiles.length);
            for( File shpFile : shpfiles ) {
                String name = FileUtilities.getNameWithoutExtention(shpFile);

                if (db.hasTable(name)) {
                    pm.errorMessage("Table already existing: " + name);
                    continue;
                }

                SpatialDbsImportUtils.createTableFromShp(db, shpFile, name, null, false);
                SpatialDbsImportUtils.importShapefile(db, shpFile, name, -1, false, pm);

                Style style = SldUtilities.getStyleFromFile(shpFile);
                if (style != null) {
                    String sld = SldUtilities.styleToString(style);
                    ((GeopackageDb) db).updateSldStyle(name, sld);
                }
                pm.worked(1);
            }
            pm.done();
        }

    }

    public static void main( String[] args ) throws Exception {
        GeopaparazziGeopackageCreator c = new GeopaparazziGeopackageCreator();
        c.inGeopackage = "/Users/hydrologis/Dropbox/hydrologis/data/example_data/naturalearth_italy/ne.gpkg";
        c.inShapefilesFolder = "/Users/hydrologis/Dropbox/hydrologis/data/example_data/naturalearth_italy/";
        c.process();
    }
}
