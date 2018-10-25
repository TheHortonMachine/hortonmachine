package org.hortonmachine.gears.modules;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.hortonmachine.gears.io.disktree.IDiskTree;
import org.hortonmachine.gears.utils.HMTestCase;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.io.WKTReader;
/**
 * Test {@link IDiskTree} reader and writer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestDiskTreeIO extends HMTestCase {

    // public void testDiskTreeRead() throws Exception {
    // DiskTreeReader r = new DiskTreeReader("D:/TMP/milano/geometries.qjts");
    // long t1 = System.currentTimeMillis();
    // Quadtree readIndex = r.readIndex();
    // long t2 = System.currentTimeMillis();
    // System.out.println((t2 - t1));
    //
    // List queryAll = readIndex.queryAll();
    // long t3 = System.currentTimeMillis();
    // System.out.println((t3 - t2));
    //
    // long[] o = (long[]) queryAll.get(300);
    // long t4 = System.currentTimeMillis();
    // System.out.println((t4 - t3));
    //
    // Geometry pickGeometry = r.pickGeometry(o[0], o[1]);
    // Object userData = pickGeometry.getUserData();
    // long t5 = System.currentTimeMillis();
    // System.out.println((t5 - t4));
    // System.out.println(pickGeometry);
    // System.out.println(userData);
    //
    // }

    // public void testDiskTreeWrite() throws Exception {
    // SimpleFeatureCollection readVector = OmsVectorReader.readVector("D:/TMP/milano/localita.shp");
    // List<FeatureMate> mates = FeatureUtilities.featureCollectionToMatesList(readVector);
    //
    // DiskTreeWriter w = new DiskTreeWriter("D:/TMP/milano/geometries_buffer0_simpl000002.qjts");
    //
    // List<Geometry> geoms = new ArrayList<Geometry>();
    // for( FeatureMate featureMate : mates ) {
    // String id = featureMate.getAttribute("DI_ID", String.class);
    // String name = featureMate.getAttribute("DI_NAME", String.class);
    // String idStr = id + "@" + name;
    //
    // Geometry geometry = featureMate.getGeometry();
    //
    // int n = geometry.getNumGeometries();
    // for( int i = 0; i < n; i++ ) {
    // Geometry geometryN = geometry.getGeometryN(i);
    // DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(geometryN);
    // dpSimplifier.setDistanceTolerance(0.000002);
    // geometryN = dpSimplifier.getResultGeometry();
    //
    // geometryN.setUserData(idStr);
    // geoms.add(geometryN);
    // }
    //
    // // geometry = geometry.buffer(0);
    // // DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(geometry);
    // // dpSimplifier.setDistanceTolerance(0.00002);
    // // geometry = dpSimplifier.getResultGeometry();
    //
    // // geometry.setUserData(idStr);
    // // geoms.add(geometry);
    // }
    //
    // Geometry[] array = geoms.toArray(new Geometry[0]);
    //
    // w.writeGeometries(array);
    //
    // SimpleFeatureCollection newCollection = new DefaultFeatureCollection();
    // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
    // b.setName("typename");
    // b.setCRS(DefaultGeographicCRS.WGS84);
    // b.add("the_geom", MultiPolygon.class);
    // SimpleFeatureType type = b.buildFeatureType();
    // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
    //
    // for( Geometry g : array ) {
    // Object[] values = new Object[]{g};
    // builder.addAll(values);
    // SimpleFeature feature = builder.buildFeature(null);
    // newCollection.add(feature);
    // }
    //
    // OmsVectorWriter.writeVector("D:/TMP/milano/geometries_buffer0_simpl000002.shp", newCollection);
    //
    // }

    public void testSerialization() throws Exception {

        WKTReader r = new WKTReader();
        Geometry pol = r.read("POLYGON ((210 350, 230 310, 290 350, 290 350, 210 350))");
        pol.setUserData(new Integer(1));
        Envelope polEnvelope = pol.getEnvelopeInternal();
        Geometry line = r.read("LINESTRING (50 380, 90 210, 180 160, 240 40, 240 40)");
        line.setUserData(new Integer(2));
        Envelope lineEnvelope = line.getEnvelopeInternal();
        Geometry point = r.read("POINT (130 120)");
        point.setUserData(new Integer(3));
        Envelope pointEnvelope = point.getEnvelopeInternal();
        pointEnvelope.expandBy(0.000001);

        Geometry[] geoms = {point, line, pol};

        // put geometries serialized in quadtree
        Quadtree tree = new Quadtree();
        for( int i = 0; i < geoms.length; i++ ) {
            Geometry geometry = geoms[i];
            byte[] geomBytes = serialize(geometry);

            Envelope envelope = geometry.getEnvelopeInternal();
            tree.insert(envelope, geomBytes);
        }

        // serialize tree
        byte[] serializedTree = serialize(tree);

        // deserialize tree
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serializedTree));
        Quadtree deserializedTree = (Quadtree) in.readObject();

        // query the polygon envelope and extract the geometries
        List geomList = deserializedTree.query(polEnvelope);
        assertTrue(geomList.size() == 2);
        byte[] polygonGeomObj = (byte[]) geomList.get(0);
        in = new ObjectInputStream(new ByteArrayInputStream(polygonGeomObj));
        Geometry geometry = (Geometry) in.readObject();
        Integer userData = (Integer) geometry.getUserData();
        if (userData == 2) {
            assertEquals("LineString", geometry.getGeometryType());
        }
        if (userData == 1) {
            assertEquals("Polygon", geometry.getGeometryType());
        }

    }

    private static byte[] serialize( Object obj ) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(obj);
        out.close();
        byte[] treeBytes = bos.toByteArray();
        return treeBytes;
    }
}
