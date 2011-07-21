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
package org.jgrasstools.gears.io.shapefile;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.opengis.feature.simple.SimpleFeatureType;

@Description("Utility class for writing geotools featurecollections to shapefile.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Shapefile, Feature, Vector, Writing")
@Label(JGTConstants.FEATUREWRITER)
@Status(Status.CERTIFIED)
@UI(JGTConstants.HIDE_UI_HINT)
@License("General Public License Version 3 (GPLv3)")
public class ShapefileFeatureWriter extends JGTModel {

    @Description("The feature collection to write.")
    @In
    public SimpleFeatureCollection geodata = null;

    @Description("The shapefile to which to write to.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;
    
    @Description("The feature type. It's mandatory only if you want to write down an empty FeatureCollection")
    @In
    public SimpleFeatureType pType = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    private boolean hasWritten = false;

    @Execute
    public void writeFeatureCollection() throws IOException {
        if (!concatOr(!hasWritten, doReset)) {
            return;
        }

        pm.beginTask("Writing features to shapefile...", -1);

        if (!file.endsWith(".shp")) {
            file = file + ".shp";
        }
        File shapeFile = new File(file);
        DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", shapeFile.toURI().toURL());
        // params.put("create spatial index", Boolean.TRUE);
        if (geodata != null && geodata.size() != 0) {
            pType = geodata.getSchema();
        }
        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema(pType);
        newDataStore.forceSchemaCRS(pType.getCoordinateReferenceSystem());

        Transaction transaction = new DefaultTransaction("create");
        String typeName = newDataStore.getTypeNames()[0];
        SimpleFeatureStore featureStore = (SimpleFeatureStore) newDataStore.getFeatureSource(typeName);

        featureStore.setTransaction(transaction);
        try {
            if (geodata == null) {
                featureStore.addFeatures(FeatureCollections.newCollection());
            } else {
                featureStore.addFeatures(geodata);
            }
            transaction.commit();
        } catch (Exception problem) {
            transaction.rollback();
            throw new IOException(problem.getLocalizedMessage());
        } finally {
            transaction.close();
            pm.done();
        }

        hasWritten = true;
    }

    public static void writeShapefile( String path, SimpleFeatureCollection featureCollection ) throws IOException {
        ShapefileFeatureWriter writer = new ShapefileFeatureWriter();
        writer.file = path;
        writer.geodata = featureCollection;
        writer.writeFeatureCollection();
    }

    public static void writeEmptyShapefile( String path, SimpleFeatureType schema ) throws IOException {
        ShapefileFeatureWriter writer = new ShapefileFeatureWriter();
        writer.file = path;
        writer.pType = schema;
        writer.writeFeatureCollection();
    }

}
