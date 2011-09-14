package org.jgrasstools.gears.modules;

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.disktree.DiskTreeReader;
import org.jgrasstools.gears.io.disktree.DiskTreeWriter;
import org.jgrasstools.gears.io.disktree.IDiskTree;
import org.jgrasstools.gears.io.disktree.jtstmp.Quadtree;
import org.jgrasstools.gears.io.vectorreader.VectorReader;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;

import com.vividsolutions.jts.geom.Geometry;
/**
 * Test {@link IDiskTree} reader and writer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestDiskTreeIO extends HMTestCase {

//    public void testDiskTreeRead() throws Exception {
//        DiskTreeReader r = new DiskTreeReader("D:/TMP/milano/geometries.qjts");
//        long t1 = System.currentTimeMillis();
//        Quadtree readIndex = r.readIndex();
//        long t2 = System.currentTimeMillis();
//        System.out.println((t2 - t1));
//
//        List queryAll = readIndex.queryAll();
//        long t3 = System.currentTimeMillis();
//        System.out.println((t3 - t2));
//
//        long[] o = (long[]) queryAll.get(300);
//        long t4 = System.currentTimeMillis();
//        System.out.println((t4 - t3));
//
//        Geometry pickGeometry = r.pickGeometry(o[0], o[1]);
//        Object userData = pickGeometry.getUserData();
//        long t5 = System.currentTimeMillis();
//        System.out.println((t5 - t4));
//        System.out.println(pickGeometry);
//        System.out.println(userData);
//
//    }

//    public void testDiskTreeWrite() throws Exception {
//        SimpleFeatureCollection readVector = VectorReader.readVector("D:/TMP/milano/localita.shp");
//        List<FeatureMate> mates = FeatureUtilities.featureCollectionToMatesList(readVector);
//
//        DiskTreeWriter w = new DiskTreeWriter("D:/TMP/milano/geometries.qjts");
//
//        List<Geometry> geoms = new ArrayList<Geometry>();
//        for( FeatureMate featureMate : mates ) {
//            String id = featureMate.getAttribute("DI_ID", String.class);
//            String name = featureMate.getAttribute("DI_NAME", String.class);
//            String idStr = id + "@" + name;
//
//            Geometry geometry = featureMate.getGeometry();
//            geometry.setUserData(idStr);
//            geoms.add(geometry);
//        }
//
//        Geometry[] array = geoms.toArray(new Geometry[0]);
//
//        w.writeGeometries(array);
//    }
}
