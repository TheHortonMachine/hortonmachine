package org.hortonmachine;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.postgis.PostgisGeometryParser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.postgis.GeometryBuilder;
import org.postgis.binary.BinaryWriter;

public class Main {

    public static void main( String[] args ) throws Exception {
//        ASpatialDb db = EDb.POSTGIS.getSpatialDb();
//        boolean opened = db.open("ocahost:5432/test", "postgres", "postgres");
//        if (opened) {
//            db.executeInsertUpdateDeleteSql("drop table if exists test2 cascade;");
//            db.executeInsertUpdateDeleteSql("CREATE TABLE test2 (name varchar, geom geometry(geometry, 4326));");
//
//        }

        Geometry geom = new WKTReader().read("POINT(-2 2)");
        BinaryWriter bw = new BinaryWriter();
        
        org.postgis.Geometry pgGeometry = GeometryBuilder.geomFromString(geom.toText());
        pgGeometry.setSrid(4326);
        
        System.out.println(bw.writeHexed(pgGeometry));
//        byte[] writeBinary = bw.writeBinary(pgGeometry);
//        for( byte b : writeBinary ) {
//            System.out.println(b);
//        }
//        System.out.println(writeBinary);
//        
//        
//        PostgisGeometryParser p = new PostgisGeometryParser();
//        Object sqlObject = p.toSqlObject(geom);
//        System.out.println(sqlObject);
    }

}
