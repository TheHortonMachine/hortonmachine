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
package org.jgrasstools.gears.modules.v.vectorclipper;

import static org.jgrasstools.gears.libs.modules.Variables.DIFFERENCE;
import static org.jgrasstools.gears.libs.modules.Variables.INTERSECTION;
import static org.jgrasstools.gears.libs.modules.Variables.SYMDIFFERENCE;
import static org.jgrasstools.gears.libs.modules.Variables.UNION;

import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollections;
import org.jgrasstools.gears.io.vectorreader.VectorReader;
import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.time.EggClock;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.strtree.STRtree;

@Description("A vector clipping module.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("JTS, Clip, Vector")
@Status(Status.EXPERIMENTAL)
@Name("clipper")
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class VectorClipper extends JGTModel {

    @Description("The vector map to be clipped.")
    @In
    public SimpleFeatureCollection inMap = null;

    @Description("The clipping vector map.")
    @In
    public SimpleFeatureCollection inClipper = null;

    @Description("Max threads to use (default 1)")
    @In
    public int pMaxThreads = 1;

    @Description("The resulting vector map.")
    @Out
    public SimpleFeatureCollection outMap = null;

    @Execute
    public void process() throws Exception {
        checkNull(inMap, inClipper);

        if (!GeometryUtilities.isPolygon(inClipper.getSchema().getGeometryDescriptor())) {
            throw new ModelsIllegalargumentException("The clipping geometry needs to be polygon.", this);
        }

        pm.beginTask("Indexing geometries...", IJGTProgressMonitor.UNKNOWN);
        STRtree geomsTree = new STRtree(2000);
        SimpleFeatureIterator featuresToClip = inMap.features();
        while( featuresToClip.hasNext() ) {
            SimpleFeature feature = featuresToClip.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            geomsTree.insert(geometry.getEnvelopeInternal(), feature);
        }
        pm.done();

        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(pMaxThreads);
        final ConcurrentLinkedQueue<SimpleFeature> newFeatures = new ConcurrentLinkedQueue<SimpleFeature>();
        List<Geometry> clipperGeoms = FeatureUtilities.featureCollectionToGeometriesList(inClipper, false, null);
        int index = 1;
        int clipperCount = clipperGeoms.size();
        for( Geometry clipperGeom : clipperGeoms ) {
            pm.message("Working on geometry " + (index++) + " of " + clipperCount);
            List< ? > result = geomsTree.query(clipperGeom.getEnvelopeInternal());

            int size = result.size();
            int splitSize = (int) Math.ceil(size / (double) pMaxThreads);
            final CountDownLatch latch = new CountDownLatch(pMaxThreads);
            pm.beginTask("Clipping...", pMaxThreads);
            for( int i = 0; i < size; i = i + splitSize + 1 ) {
                int end = i + splitSize;
                if (end > size) {
                    end = size - 1;
                }
                // pm.errorMessage(i + "/" + end + "/" + size);
                final List< ? > subList = result.subList(i, end);
                final Geometry cGeom = clipperGeom;
                Runnable runner = new Runnable(){
                    public void run() {
                        for( Object resultObj : subList ) {
                            SimpleFeature feature = (SimpleFeature) resultObj;
                            Geometry geometry = (Geometry) feature.getDefaultGeometry();

                            if (cGeom.intersects(geometry)) {
                                Geometry intersectionGeom = cGeom.intersection(geometry);
                                feature.setDefaultGeometry(intersectionGeom);
                                newFeatures.add(feature);
                            }
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

        outMap = FeatureCollections.newCollection();
        outMap.addAll(newFeatures);
    }

    public static void main( String[] args ) throws Exception {
        PrintStream ps = System.out;
        EggClock c = new EggClock("", "\n");
        c.startAndPrint(ps);

        SimpleFeatureCollection clip = VectorReader
                .readVector("D:/TMP/CLIPPING_CONTEST/ContourClipTest/StudyArea1MileBuffer.shp");
        SimpleFeatureCollection data = VectorReader.readVector("D:/TMP/CLIPPING_CONTEST/ContourClipTest/Contours20Ft.shp");

        c.printTimePassedInSeconds(ps);

        VectorClipper clipper = new VectorClipper();
        clipper.inMap = data;
        clipper.inClipper = clip;
        clipper.pMaxThreads = 20;
        clipper.process();
        SimpleFeatureCollection outMap2 = clipper.outMap;

        c.printTimePassedInSeconds(ps);

        VectorWriter.writeVector("D:/TMP/CLIPPING_CONTEST/ContourClipTest/clipped.shp", outMap2);

        c.printTimePassedInSeconds(ps);
    }

}
