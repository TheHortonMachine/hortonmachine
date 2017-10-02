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
public enum Units {
    CFS("CFS", "Cubic feet per second"), //
    GPM("GPM", "Gallons per minute"), //
    MGD("MGD", "Million gallons per day"), //
    IMGD("IMGD", "Imperial Million gallons per day"), //
    AFD("AFD", "Acre-feet per day"), //
    LPS("LPS", "Liters per second"), //
    LPM("LPM", "Liters per minute"), //
    MLD("MLD", "Million liters per day"), //
    CMH("CMH", "Cubic meters per hour"), //
    CMD("CMD", "Cubic meters per day");

    private String name;
    private String description;
    Units( String name, String description ) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
