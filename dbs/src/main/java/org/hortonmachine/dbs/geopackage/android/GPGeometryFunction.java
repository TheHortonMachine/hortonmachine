package org.hortonmachine.dbs.geopackage.android;

import java.io.IOException;
import java.sql.SQLException;

import org.hortonmachine.dbs.geopackage.geom.GeoPkgGeomReader;

import jsqlite.Function;
import jsqlite.FunctionContext;

public abstract class GPGeometryFunction implements Function {

    public abstract Object execute( GeoPkgGeomReader reader ) throws Exception;

    @Override
    public void function( FunctionContext fc, String[] args ) {

        try {
            if (args.length != 1) {
                throw new SQLException("Geometry Function expects one argument.");
            }
            byte[] value = args[0].getBytes();

            Object res;
            try {
                res = execute(new GeoPkgGeomReader(value));
            } catch (Exception e) {
                throw new SQLException(e);
            }

            if (res == null) {
                fc.set_result((byte[]) null);
            } else if (res instanceof Integer) {
                fc.set_result((Integer) res);
            } else if (res instanceof Double) {
                fc.set_result((Double) res);
            } else if (res instanceof String) {
                fc.set_result((String) res);
            } else if (res instanceof Long) {
                fc.set_result((Long) res);
            } else if (res instanceof byte[]) {
                fc.set_result((byte[]) res);
            } else if (res instanceof Boolean) {
                fc.set_result((Boolean) res ? 1 : 0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            fc.set_error("ERROR in Geometry function with arg(" + args[0] + "):" + e);
        }

    }

    @Override
    public void step( FunctionContext fc, String[] args ) {
    }

    @Override
    public void last_step( FunctionContext fc ) {
    }

}
