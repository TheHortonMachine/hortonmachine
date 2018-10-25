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
package org.hortonmachine.gears.modules.v.vectorclipper;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_IN_CLIPPER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_IN_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_OUT_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_P_MAX_THREADS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORCLIPPER_STATUS;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description(OMSVECTORCLIPPER_DESCRIPTION)
@Documentation(OMSVECTORCLIPPER_DOCUMENTATION)
@Author(name = OMSVECTORCLIPPER_AUTHORNAMES, contact = OMSVECTORCLIPPER_AUTHORCONTACTS)
@Keywords(OMSVECTORCLIPPER_KEYWORDS)
@Label(OMSVECTORCLIPPER_LABEL)
@Name(OMSVECTORCLIPPER_NAME)
@Status(OMSVECTORCLIPPER_STATUS)
@License(OMSVECTORCLIPPER_LICENSE)
public class OmsVectorClipper extends HMModel {

    @Description(OMSVECTORCLIPPER_IN_MAP_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(OMSVECTORCLIPPER_IN_CLIPPER_DESCRIPTION)
    @In
    public SimpleFeatureCollection inClipper = null;

    @Description(OMSVECTORCLIPPER_P_MAX_THREADS_DESCRIPTION)
    @In
    public int pMaxThreads = 1;

    @Description(OMSVECTORCLIPPER_OUT_MAP_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;

    @Execute
    public void process() throws Exception {
        checkNull(inMap, inClipper);

        if (!EGeometryType.isPolygon(inClipper.getSchema().getGeometryDescriptor())) {
            throw new ModelsIllegalargumentException("The clipping geometry needs to be polygon.", this, pm);
        }

        pm.beginTask("Indexing geometries...", IHMProgressMonitor.UNKNOWN);
        STRtree geomsTree = new STRtree(2000);
        SimpleFeatureIterator featuresToClip = inMap.features();
        while( featuresToClip.hasNext() ) {
            SimpleFeature feature = featuresToClip.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            geomsTree.insert(geometry.getEnvelopeInternal(), feature);
        }
        pm.done();
        // List<SimpleFeature> inData = FeatureUtilities.featureCollectionToList(inMap);
        // int size = inData.size();

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(pMaxThreads);
        final ConcurrentLinkedQueue<SimpleFeature> newFeatures = new ConcurrentLinkedQueue<SimpleFeature>();
        List<Geometry> clipperGeoms = FeatureUtilities.featureCollectionToGeometriesList(inClipper, false, null);
        int index = 1;
        int clipperCount = clipperGeoms.size();
        for( Geometry clipperGeom : clipperGeoms ) {
            pm.message("Working on geometry " + (index++) + " of " + clipperCount);

            final List< ? > result = geomsTree.query(clipperGeom.getEnvelopeInternal());

            int size = result.size();

            int splitSize = (int) Math.ceil(size / (double) pMaxThreads);
            final CountDownLatch latch = new CountDownLatch(pMaxThreads);
            pm.beginTask("Clipping...", pMaxThreads);
            for( int i = 0; i < size; i = i + splitSize + 1 ) {
                int endIndex = i + splitSize;
                if (endIndex > size) {
                    endIndex = size - 1;
                }
                final PreparedGeometry cGeom = PreparedGeometryFactory.prepare(clipperGeom);

                final int start = i;
                final int end = endIndex;
                Runnable runner = new Runnable(){
                    public void run() {
                        // pm.errorMessage(start + "->" +end);
                        try {
                            for( int j = start; j <= end; j++ ) {
                                SimpleFeature feature = (SimpleFeature) result.get(j);
                                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                                if (cGeom.intersects(geometry)) {
                                    Geometry intersectionGeom = cGeom.getGeometry().intersection(geometry);
                                    feature.setDefaultGeometry(intersectionGeom);
                                    newFeatures.add(feature);
                                }
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        latch.countDown();
                        pm.worked(1);
                    }
                };
                fixedThreadPool.execute(runner);
            }
            latch.await();
            pm.done();
        }
        try {
            fixedThreadPool.shutdown();
            fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
            fixedThreadPool.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        outMap = new DefaultFeatureCollection();
        ((DefaultFeatureCollection) outMap).addAll(newFeatures);
    }

}
