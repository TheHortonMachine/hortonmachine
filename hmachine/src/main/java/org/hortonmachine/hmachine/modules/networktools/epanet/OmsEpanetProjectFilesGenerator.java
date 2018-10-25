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
package org.hortonmachine.hmachine.modules.networktools.epanet;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_inFolder_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPROJECTFILESGENERATOR_pCode_DESCRIPTION;

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
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.IEpanetType;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Junctions;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Pipes;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Pumps;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Reservoirs;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Tanks;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.EpanetFeatureTypes.Valves;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

@Description(OMSEPANETPROJECTFILESGENERATOR_DESCRIPTION)
@Author(name = OMSEPANETPROJECTFILESGENERATOR_AUTHORNAMES, contact = OMSEPANETPROJECTFILESGENERATOR_AUTHORCONTACTS)
@Keywords(OMSEPANETPROJECTFILESGENERATOR_KEYWORDS)
@Label(OMSEPANETPROJECTFILESGENERATOR_LABEL)
@Name(OMSEPANETPROJECTFILESGENERATOR_NAME)
@Status(OMSEPANETPROJECTFILESGENERATOR_STATUS)
@License(OMSEPANETPROJECTFILESGENERATOR_LICENSE)
public class OmsEpanetProjectFilesGenerator extends HMModel {

    @Description(OMSEPANETPROJECTFILESGENERATOR_inFolder_DESCRIPTION)
    @In
    public String inFolder = null;

    @Description(OMSEPANETPROJECTFILESGENERATOR_pCode_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Execute
    public void process() throws Exception {
        checkNull(inFolder, pCode);

        CoordinateReferenceSystem crs = CrsUtilities.getCrsFromEpsg(pCode, null);

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
            featureStore.addFeatures(new DefaultFeatureCollection());
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
            featureStore.addFeatures(new DefaultFeatureCollection());
            transaction.commit();
        } catch (Exception problem) {
            problem.printStackTrace();
            transaction.rollback();
        } finally {
            transaction.close();
        }
    }
}
