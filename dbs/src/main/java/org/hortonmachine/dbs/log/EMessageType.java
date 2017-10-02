/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.dbs.log;

/**
 * An enumeration of message types for logging.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum EMessageType {
    ALL(0), INFO(1), WARNING(2), ERROR(3), ACCESS(4), DEBUG(5);

    private int code;

    private EMessageType( int code ) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static EMessageType fromCode( int code ) {
        switch( code ) {
        case 0:
            return ALL;
        case 1:
            return INFO;
        case 2:
            return WARNING;
        case 3:
            return ERROR;
        case 4:
            return ACCESS;
        case 5:
            return DEBUG;
        default:
            return ALL;
        }
    }

}
