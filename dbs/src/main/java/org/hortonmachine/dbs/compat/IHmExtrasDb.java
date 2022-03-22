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
package org.hortonmachine.dbs.compat;

import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * 
 * Add style and form support to the database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface IHmExtrasDb {
    public static final String HM_STYLES_TABLE = "hm_styles";
    public static final String HM_FORMS_TABLE = "hm_forms";
    public static final String QGIS_STYLES_TABLE = "layer_styles";

    public static final String STYLE_SIMPLIFIED_FIELD = "simplified";
    public static final String STYLE_SLD_FIELD = "sld";
    public static final String STYLE_TABLENAME_FIELD = "tablename";

    public static final String STYLE_QGIS_TABLENAME_FIELD = "f_table_name";
    public static final String STYLE_QGIS_SLD_FIELD = "styleSLD";

    public static final String FORMS_TABLENAME_FIELD = "tablename";
    public static final String FORMS_FIELD = "forms";

    public String getSldString( SqlName tableName ) throws Exception;

    public void updateSldStyle( SqlName tableName, String sldString ) throws Exception;

    public String getFormString( SqlName tableName ) throws Exception;

    public void updateForm( SqlName tableName, String form ) throws Exception;

    default public void checkStyleTable( ADb db ) throws Exception {
        SqlName hmStylesTable = SqlName.m(HM_STYLES_TABLE);
        if (!db.hasTable(hmStylesTable)) {
            ADatabaseSyntaxHelper dt = db.getType().getDatabaseSyntaxHelper();
            db.createTable(hmStylesTable, //
                    STYLE_TABLENAME_FIELD + " " + dt.TEXT(), //
                    STYLE_SLD_FIELD + " " + dt.TEXT(), //
                    STYLE_SIMPLIFIED_FIELD + " " + dt.TEXT() //
            );
            db.createIndex(hmStylesTable, STYLE_TABLENAME_FIELD, true);
        }
    }

    default public void checkFormTable( ADb db ) throws Exception {
        SqlName hmFormsTable = SqlName.m(HM_FORMS_TABLE);
        if (!db.hasTable(hmFormsTable)) {
            ADatabaseSyntaxHelper dt = db.getType().getDatabaseSyntaxHelper();
            db.createTable(hmFormsTable, //
                    FORMS_TABLENAME_FIELD + " " + dt.TEXT(), //
                    FORMS_FIELD + " " + dt.TEXT());
            db.createIndex(hmFormsTable, FORMS_TABLENAME_FIELD, true);
        }
    }

    default String getSldStringInternal( ADb db, SqlName tableName ) throws Exception {
        checkStyleTable(db);

        return db.execOnConnection(connection -> {
            String sql = "select " + STYLE_SLD_FIELD + " from " + HM_STYLES_TABLE + " where lower(" + STYLE_TABLENAME_FIELD
                    + ")='" + tableName.name.toLowerCase() + "'";
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
            if (db.hasTable(SqlName.m(QGIS_STYLES_TABLE))) {
                // check is maybe there is a qgis style available
                sql = "select " + STYLE_QGIS_SLD_FIELD + " from " + QGIS_STYLES_TABLE + " where lower("
                        + STYLE_QGIS_TABLENAME_FIELD + ")='" + tableName.name.toLowerCase() + "'";
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                    if (rs.next()) {
                        String sld = rs.getString(1);
                        if (sld != null) {
                            if (sld.trim().length() == 0)
                                return null;
                        }
                        return sld;
                    }
                }
            }
            return null;
        });
    }

    default void updateSldStyleInternal( ADb db, SqlName tableName, String sldString ) throws Exception {
        checkStyleTable(db);
        Long count = db.getLong("select count(*) from " + HM_STYLES_TABLE + " where " + STYLE_TABLENAME_FIELD + "='"
                + tableName.name + "'");
        String sql;
        if (count == 0) {
            sql = "INSERT INTO " + HM_STYLES_TABLE + "(" + STYLE_TABLENAME_FIELD + ", " + STYLE_SLD_FIELD + ") VALUES(?,?)";
            db.execOnConnection(connection -> {
                try (IHMPreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, tableName.name);
                    pstmt.setString(2, sldString);
                    pstmt.executeUpdate();
                }
                return null;
            });
        } else {
            sql = "update " + HM_STYLES_TABLE + " set " + STYLE_SLD_FIELD + "=? where " + STYLE_TABLENAME_FIELD + "=?";
            db.execOnConnection(connection -> {
                try (IHMPreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, sldString);
                    pstmt.setString(2, tableName.name);
                    pstmt.executeUpdate();
                }
                return null;
            });
        }
    }

    default String getFormStringInternal( ADb db, SqlName tableName ) throws Exception {
        checkFormTable(db);

        return db.execOnConnection(connection -> {
            String sql = "select " + FORMS_FIELD + " from " + HM_FORMS_TABLE + " where lower(" + FORMS_TABLENAME_FIELD + ")='"
                    + tableName.name.toLowerCase() + "'";
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
            return null;
        });
    }

    default void updateFormsInternal( ADb db, SqlName tableName, String formString ) throws Exception {
        checkFormTable(db);

        Long count = db.getLong("select count(*) from " + HM_FORMS_TABLE + " where " + FORMS_TABLENAME_FIELD + "='"
                + tableName.name + "'");
        String sql;
        if (count == 0) {
            sql = "INSERT INTO " + HM_FORMS_TABLE + "(" + FORMS_TABLENAME_FIELD + ", " + FORMS_FIELD + ") VALUES(?,?)";
            db.execOnConnection(connection -> {
                try (IHMPreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, tableName.name);
                    pstmt.setString(2, formString);
                    pstmt.executeUpdate();
                }
                return null;
            });
        } else {
            sql = "update " + HM_FORMS_TABLE + " set " + FORMS_FIELD + "=? where " + FORMS_TABLENAME_FIELD + "=?";
            db.execOnConnection(connection -> {
                try (IHMPreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, formString);
                    pstmt.setString(2, tableName.name);
                    pstmt.executeUpdate();
                }
                return null;
            });
        }
    }

}
