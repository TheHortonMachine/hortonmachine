/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
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

package org.hortonmachine.gears.io.geopaparazzi.styles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.utils.BasicStyle;

import static org.hortonmachine.dbs.utils.BasicStyle.*;

/**
 * geopaparazzi related database utilities.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziDatabaseProperties implements ISpatialiteTableAndFieldsNames {

    /**
     * The complete list of fields in the properties table.
     */
    public static List<String> PROPERTIESTABLE_FIELDS_LIST;

    static {
        List<String> fieldsList = new ArrayList<String>();
        fieldsList.add(ID);
        fieldsList.add(NAME);
        fieldsList.add(SIZE);
        fieldsList.add(FILLCOLOR);
        fieldsList.add(STROKECOLOR);
        fieldsList.add(FILLALPHA);
        fieldsList.add(STROKEALPHA);
        fieldsList.add(SHAPE);
        fieldsList.add(WIDTH);
        fieldsList.add(LABELSIZE);
        fieldsList.add(LABELFIELD);
        fieldsList.add(LABELVISIBLE);
        fieldsList.add(ENABLED);
        fieldsList.add(ORDER);
        fieldsList.add(DASH);
        fieldsList.add(MINZOOM);
        fieldsList.add(MAXZOOM);
        fieldsList.add(DECIMATION);
        fieldsList.add(THEME);
        PROPERTIESTABLE_FIELDS_LIST = Collections.unmodifiableList(fieldsList);
    }

    /**
     * Create the properties table.
     *
     * @param database the db to use.
     * @throws Exception 
     */
    public static void createPropertiesTable( ASpatialDb database ) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(PROPERTIESTABLE);
        sb.append(" (");
        sb.append(ID);
        sb.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sb.append(NAME).append(" TEXT, ");
        sb.append(SIZE).append(" REAL, ");
        sb.append(FILLCOLOR).append(" TEXT, ");
        sb.append(STROKECOLOR).append(" TEXT, ");
        sb.append(FILLALPHA).append(" REAL, ");
        sb.append(STROKEALPHA).append(" REAL, ");
        sb.append(SHAPE).append(" TEXT, ");
        sb.append(WIDTH).append(" REAL, ");
        sb.append(LABELSIZE).append(" REAL, ");
        sb.append(LABELFIELD).append(" TEXT, ");
        sb.append(LABELVISIBLE).append(" INTEGER, ");
        sb.append(ENABLED).append(" INTEGER, ");
        sb.append(ORDER).append(" INTEGER,");
        sb.append(DASH).append(" TEXT,");
        sb.append(MINZOOM).append(" INTEGER,");
        sb.append(MAXZOOM).append(" INTEGER,");
        sb.append(DECIMATION).append(" REAL,");
        sb.append(THEME).append(" TEXT");
        sb.append(" );");
        String query = sb.toString();
        database.executeInsertUpdateDeleteSql(query);
    }

    /**
     * Create a default properties table for a spatial table.
     *
     * @param database the db to use. If <code>null</code>, the style is not inserted in the db.
     * @param spatialTableUniqueName the spatial table's unique name to create the property record for.
     * @return 
     * @return the created style object.
     * @throws Exception if something goes wrong.
     */
    public static BasicStyle createDefaultPropertiesForTable( ASpatialDb database, String spatialTableUniqueName,
            String spatialTableLabelField ) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("insert into ").append(PROPERTIESTABLE);
        sbIn.append(" ( ");
        sbIn.append(NAME).append(" , ");
        sbIn.append(SIZE).append(" , ");
        sbIn.append(FILLCOLOR).append(" , ");
        sbIn.append(STROKECOLOR).append(" , ");
        sbIn.append(FILLALPHA).append(" , ");
        sbIn.append(STROKEALPHA).append(" , ");
        sbIn.append(SHAPE).append(" , ");
        sbIn.append(WIDTH).append(" , ");
        sbIn.append(LABELSIZE).append(" , ");
        sbIn.append(LABELFIELD).append(" , ");
        sbIn.append(LABELVISIBLE).append(" , ");
        sbIn.append(ENABLED).append(" , ");
        sbIn.append(ORDER).append(" , ");
        sbIn.append(DASH).append(" ,");
        sbIn.append(MINZOOM).append(" ,");
        sbIn.append(MAXZOOM).append(" ,");
        sbIn.append(DECIMATION);
        sbIn.append(" ) ");
        sbIn.append(" values ");
        sbIn.append(" ( ");
        BasicStyle style = new BasicStyle();
        style.name = spatialTableUniqueName;
        style.labelfield = spatialTableLabelField;
        if (spatialTableLabelField != null && spatialTableLabelField.trim().length() > 0) {
            style.labelvisible = 1;
        }
        sbIn.append(style.insertValuesString());
        sbIn.append(" );");

        if (database != null) {
            String insertQuery = sbIn.toString();
            database.executeInsertUpdateDeleteSql(insertQuery);
        }
        return style;
    }

    /**
     * Update a style definition.
     *
     * @param database the db to use.
     * @param style    the {@link BasicStyle} to set.
     * @throws Exception if something goes wrong.
     */
    public static void updateStyle( ASpatialDb database, BasicStyle style ) throws Exception {
        StringBuilder sbIn = new StringBuilder();
        sbIn.append("update ").append(PROPERTIESTABLE);
        sbIn.append(" set ");
        // sbIn.append(NAME).append("='").append(style.name).append("' , ");
        sbIn.append(SIZE).append("=?,");
        sbIn.append(FILLCOLOR).append("=?,");
        sbIn.append(STROKECOLOR).append("=?,");
        sbIn.append(FILLALPHA).append("=?,");
        sbIn.append(STROKEALPHA).append("=?,");
        sbIn.append(SHAPE).append("=?,");
        sbIn.append(WIDTH).append("=?,");
        sbIn.append(LABELSIZE).append("=?,");
        sbIn.append(LABELFIELD).append("=?,");
        sbIn.append(LABELVISIBLE).append("=?,");
        sbIn.append(ENABLED).append("=?,");
        sbIn.append(ORDER).append("=?,");
        sbIn.append(DASH).append("=?,");
        sbIn.append(MINZOOM).append("=?,");
        sbIn.append(MAXZOOM).append("=?,");
        sbIn.append(DECIMATION).append("=?,");
        sbIn.append(THEME).append("=?");
        sbIn.append(" where ");
        sbIn.append(NAME);
        sbIn.append("='");
        sbIn.append(style.name);
        sbIn.append("';");

        Object[] objects = {style.size, style.fillcolor, style.strokecolor, style.fillalpha, style.strokealpha, style.shape,
                style.width, style.labelsize, style.labelfield, style.labelvisible, style.enabled, style.order, style.dashPattern,
                style.minZoom, style.maxZoom, style.decimationFactor, style.getTheme()};

        String updateQuery = sbIn.toString();
        database.executeInsertUpdateDeletePreparedSql(updateQuery, objects);
    }

}
