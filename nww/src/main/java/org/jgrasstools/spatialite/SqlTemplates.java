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
package org.jgrasstools.spatialite;

import java.util.LinkedHashMap;

/**
 * Simple queries templates.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SqlTemplates {

    public static final LinkedHashMap<String, String> templatesMap = new LinkedHashMap<String, String>();
    static {
        templatesMap.put("simple select", "select * from TABLENAME");
        templatesMap.put("geometry select", "select ST_AsBinary(the_geom) as the_geom from TABLENAME");
        templatesMap.put("where select", "select * from TABLENAME where FIELD > VALUE");
        templatesMap.put("limited select", "select * from TABLENAME limit 10");
        templatesMap.put("sorted select", "select * from TABLENAME order by FIELD asc");
        templatesMap.put("unix epoch timestamp select", "strftime('%Y-%m-%d %H:%M:%S', timestampcolumn / 1000, 'unixepoch')");
        templatesMap.put("unix epoch timestamp where select", "select * from TABLENAME where longtimestamp >= cast(strftime('%s','YYYY-MM-YY HH:mm:ss') as long)*1000");
    }

}
