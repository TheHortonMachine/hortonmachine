/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.hortonmachine.io.shapefile;

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
import oms3.annotations.License;
import oms3.annotations.Status;

import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import eu.hydrologis.jgrass.hortonmachine.libs.models.HMModel;

@Description("Utility class for writing geotools featurecollections to shapefile.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Shapefile, Feature, Vector, Writing")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class ShapefileFeatureWriter extends HMModel {
    @Description("The shapefile to which to write to.")
    @In
    public String file = null;

    @Description("The feature collection to write.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> geodata = null;

    private boolean hasWritten = false;

    @Execute
    public void writeFeatureCollection() throws IOException {
        if (!concatOr(!hasWritten, doReset)) {
            return;
        }

        if (!file.endsWith(".shp")) {
            file = file + ".shp";
        }
        File shapeFile = new File(file);
        DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", shapeFile.toURI().toURL());
        // params.put("create spatial index", Boolean.TRUE);

        SimpleFeatureType type = geodata.getSchema();
        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory
                .createNewDataStore(params);
        newDataStore.createSchema(type);
        newDataStore.forceSchemaCRS(type.getCoordinateReferenceSystem());

        Transaction transaction = new DefaultTransaction("create");
        String typeName = newDataStore.getTypeNames()[0];
        FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore
                .getFeatureSource(typeName);

        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(geodata);
            transaction.commit();
        } catch (Exception problem) {
            transaction.rollback();
            throw new IOException(problem.getLocalizedMessage());
        } finally {
            transaction.close();
        }

        hasWritten = true;
    }
}
