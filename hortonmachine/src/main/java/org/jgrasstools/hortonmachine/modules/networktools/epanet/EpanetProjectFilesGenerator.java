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
package org.jgrasstools.hortonmachine.modules.networktools.epanet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Junctions;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Pipes;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Pumps;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Reservoirs;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Tanks;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Valves;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.IEpanetType;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

@Description("Generates the base shapefiles for an epanet run.")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class EpanetProjectFilesGenerator extends JGTModel {

    @Description("The folder into which to create the base files.")
    @In
    public String inFolder = null;

    @Description("The code defining the coordinate reference system, composed by authority and code number (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Execute
    public void process() throws Exception {
        checkNull(inFolder, pCode);

        CoordinateReferenceSystem crs = CRS.decode(pCode);

        File baseFolder = new File(inFolder);

        pm.beginTask("Create epanet project shapefiles...", 7);
        pm.worked(1);
        IEpanetType[] values = Junctions.values();
        makePointLayer(values, baseFolder, crs);
        pm.worked(1);
        values = Tanks.values();
        makePointLayer(values, baseFolder, crs);
        pm.worked(1);
        values = Reservoirs.values();
        makePointLayer(values, baseFolder, crs);
        pm.worked(1);
        values = Pumps.values();
        makePointLayer(values, baseFolder, crs);
        pm.worked(1);
        values = Valves.values();
        makePointLayer(values, baseFolder, crs);
        pm.worked(1);
        values = Pipes.values();
        makeLineLayer(values, baseFolder, crs);
        pm.worked(1);
        pm.done();
    }

    private void makePointLayer( IEpanetType[] types, File baseFolder, CoordinateReferenceSystem mapCrs )
            throws MalformedURLException, IOException {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        String shapefileName = types[0].getShapefileName();
        String typeName = types[0].getName();
        b.setName(typeName);
        b.setCRS(mapCrs);
        b.add("the_geom", Point.class);
        for( IEpanetType type : types ) {
            b.add(type.getAttributeName(), type.getClazz());
        }
        SimpleFeatureType tanksType = b.buildFeatureType();
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        File file = new File(baseFolder, shapefileName);
        Map<String, Serializable> create = new HashMap<String, Serializable>();
        create.put("url", file.toURI().toURL());
        ShapefileDataStore newDataStore = (ShapefileDataStore) factory.createNewDataStore(create);
        newDataStore.createSchema(tanksType);
        Transaction transaction = new DefaultTransaction();
        SimpleFeatureStore featureStore = (SimpleFeatureStore) newDataStore.getFeatureSource();
        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(FeatureCollections.newCollection());
            transaction.commit();
        } catch (Exception problem) {
            problem.printStackTrace();
            transaction.rollback();
        } finally {
            transaction.close();
        }
    }

    private void makeLineLayer( IEpanetType[] types, File baseFolder, CoordinateReferenceSystem mapCrs )
            throws MalformedURLException, IOException {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        String shapefileName = types[0].getShapefileName();
        String typeName = types[0].getName();
        b.setName(typeName);
        b.setCRS(mapCrs);
        b.add("the_geom", LineString.class);
        for( IEpanetType type : types ) {
            b.add(type.getAttributeName(), type.getClazz());
        }
        SimpleFeatureType tanksType = b.buildFeatureType();
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        File file = new File(baseFolder, shapefileName);
        Map<String, Serializable> create = new HashMap<String, Serializable>();
        create.put("url", file.toURI().toURL());
        ShapefileDataStore newDataStore = (ShapefileDataStore) factory.createNewDataStore(create);
        newDataStore.createSchema(tanksType);
        Transaction transaction = new DefaultTransaction();
        SimpleFeatureStore featureStore = (SimpleFeatureStore) newDataStore.getFeatureSource();
        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(FeatureCollections.newCollection());
            transaction.commit();
        } catch (Exception problem) {
            problem.printStackTrace();
            transaction.rollback();
        } finally {
            transaction.close();
        }
    }
}
