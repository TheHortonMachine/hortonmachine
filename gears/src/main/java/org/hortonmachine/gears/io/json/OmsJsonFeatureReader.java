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
package org.hortonmachine.gears.io.json;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_GEODATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSJSONFEATUREREADER_UI;

import java.io.File;
import java.util.ArrayList;
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
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

@Description(OMSJSONFEATUREREADER_DESCRIPTION)
@Author(name = OMSJSONFEATUREREADER_AUTHORNAMES, contact = OMSJSONFEATUREREADER_AUTHORCONTACTS)
@Keywords(OMSJSONFEATUREREADER_KEYWORDS)
@Label(OMSJSONFEATUREREADER_LABEL)
@Name(OMSJSONFEATUREREADER_NAME)
@Status(Status.EXPERIMENTAL)
@License(OMSJSONFEATUREREADER_LICENSE)
@UI(OMSJSONFEATUREREADER_UI)
public class OmsJsonFeatureReader extends HMModel {

    @Description(OMSJSONFEATUREREADER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_JSON)
    @In
    public String file = null;

    @Description(OMSJSONFEATUREREADER_GEODATA_DESCRIPTION)
    @Out
    public SimpleFeatureCollection geodata = null;

    @Execute
    public void readFeatureCollection() throws Exception {
        if (!concatOr(geodata == null, doReset)) {
            return;
        }

        pm.beginTask("Reading json data...", IHMProgressMonitor.UNKNOWN);
        List<String> jsonStringList = FileUtilities.readFileToLinesList(new File(file));
        pm.done();
        pm.message("Found records: " + jsonStringList.size());

        List<String> namesSet = new ArrayList<String>();
        List<Class< ? >> classSet = new ArrayList<Class< ? >>();
        int checkNum = 1000;
        pm.message("Define feature type based on the first " + checkNum + " records...");
        // check first 1000 to check for names
        int count = 0;

        String latString = null;
        String lonString = null;
        for( String jsonString : jsonStringList ) {
            if (count++ > checkNum) {
                break;
            }
            JSONObject jsonObject = new JSONObject(jsonString);
            String[] names = JSONObject.getNames(jsonObject);
            for( String name : names ) {
                if (!namesSet.contains(name)) {
                    namesSet.add(name);
                    Class< ? extends Object> class1 = jsonObject.get(name).getClass();
                    classSet.add(class1);
                    pm.message("added: " + name);
                    if (name.toLowerCase().equals("lat")) {
                        latString = name;
                    }
                    if (name.toLowerCase().equals("lon")) {
                        lonString = name;
                    }
                }
            }
        }

        if (latString == null || lonString == null) {
            throw new ModelsIllegalargumentException("No lat or lon data found", this, pm);
        }

        geodata = new DefaultFeatureCollection();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("json");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        int prefix = 1;
        for( int i = 0; i < namesSet.size(); i++ ) {
            String name = namesSet.get(i);
            Class< ? > class1 = classSet.get(i);
            b.add(prefix + name, class1);
            prefix++;
        }
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        pm.beginTask("Converting json data...", jsonStringList.size());
        for( String jsonString : jsonStringList ) {
            JSONObject jsonObject = new JSONObject(jsonString);
            Object latObj = jsonObject.get(latString);
            if (!(latObj instanceof Double)) {
                pm.errorMessage("no lat found in: " + jsonString);
                pm.worked(1);
                continue;
            }
            Object lonObj = jsonObject.get(lonString);
            if (!(lonObj instanceof Double)) {
                pm.errorMessage("no lon found in: " + jsonString);
                pm.worked(1);
                continue;
            }
            double lat = (Double) latObj;
            double lon = (Double) lonObj;
            Point point = gf.createPoint(new Coordinate(lon, lat));
            List<Object> objs = new ArrayList<Object>();
            objs.add(point);
            for( String name : namesSet ) {
                Object value = null;
                if (jsonObject.has(name)) {
                    value = jsonObject.get(name);
                }
                objs.add(value);
            }
            builder.addAll(objs);
            SimpleFeature feature = builder.buildFeature(null);
            ((DefaultFeatureCollection) geodata).add(feature);
            pm.worked(1);
        }
        pm.done();

    }

    /**
     * Fast read access mode. 
     * 
     * @param path the properties file path.
     * @return the read {@link FeatureCollection}.
     * @throws Exception 
     */
    public static SimpleFeatureCollection readJsonfile( String path ) throws Exception {

        OmsJsonFeatureReader reader = new OmsJsonFeatureReader();
        reader.file = path;
        reader.readFeatureCollection();

        return reader.geodata;
    }

}
