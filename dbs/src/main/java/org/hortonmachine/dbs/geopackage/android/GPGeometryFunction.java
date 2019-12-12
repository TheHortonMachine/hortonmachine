package org.hortonmachine.dbs.geopackage.android;

import java.io.IOException;
import java.sql.SQLException;

import org.hortonmachine.dbs.geopackage.geom.GeoPkgGeomReader;

import jsqlite.Function;
import jsqlite.FunctionContext;

public abstract class GPGeometryFunction implements Function {
    
    public abstract Object execute(GeoPkgGeomReader reader) throws IOException;


    @Override
    public void function( FunctionContext fc, String[] args ) {
        
        // TODO make this work, haven't been able to link to this
//        try {
//            if (args.length != 1) {
//                throw new SQLException("Geometry Function expects one argument.");
//            }
//            String value =  args[0];
//            System.out.println(value);
//            
////            fc.set_result(null);
////            
////            Object res;
////            try {
////                res = execute(new GeoPkgGeomReader(value_blob(0)));
////            } catch (IOException e) {
////                throw new SQLException(e);
////            }
////
////            if (res == null) {
////                result();
////            } else if (res instanceof Integer) {
////                result((Integer) res);
////            } else if (res instanceof Double) {
////                result((Double) res);
////            } else if (res instanceof String) {
////                result((String) res);
////            } else if (res instanceof Long) {
////                result((Long) res);
////            } else if (res instanceof byte[]) {
////                result((byte[]) res);
////            } else if (res instanceof Boolean) {
////                result((Boolean) res ? 1 : 0);
////            }
//            
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }

    }

    @Override
    public void step( FunctionContext fc, String[] args ) {
    }

    @Override
    public void last_step( FunctionContext fc ) {
    }

}
