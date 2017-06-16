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
package org.jgrasstools.dbs.spatialite;

/**
 * Sqlite data types.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum ESqliteDataType {
    TEXT("TEXT", "text", String.class), //
    INT("INT", "int", Integer.class), //
    REAL("REAL", "real", Double.class); //

    private String name;
    private Class< ? > classToUse;
    private String lowercaseName;

    /**
     * Create the type.
     *
     * @param name         the human readable description.
     */
    ESqliteDataType( String name, String lowercaseName, Class< ? > classToUse ) {
        this.name = name;
        this.lowercaseName = lowercaseName;
        this.classToUse = classToUse;
    }

    public String getName() {
        return name;
    }
    
    public String getLowercaseName() {
        return lowercaseName;
    }

    public Class< ? > getClassToUse() {
        return classToUse;
    }

    public ESqliteDataType fromName( String name ) {
        for( ESqliteDataType type : values() ) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        throw new RuntimeException(name + " type doesn't exist");
    }
}