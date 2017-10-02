/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.networktools.epanet.core;

@SuppressWarnings("nls")
public enum ResultsNodeParameters {
    DEMAND("DEMAND", "Actual demand "), //
    HEAD("HEAD", "Hydraulic head "), //
    PRESSURE("PRESSURE", "Pressure ");

    private String key;
    private String description;
    ResultsNodeParameters( String key, String description ) {
        this.key = key;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public static ResultsNodeParameters forCode( String i ) {
        ResultsNodeParameters[] values = values();
        for( ResultsNodeParameters type : values ) {
            if (type.key.equals(i)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }
}
