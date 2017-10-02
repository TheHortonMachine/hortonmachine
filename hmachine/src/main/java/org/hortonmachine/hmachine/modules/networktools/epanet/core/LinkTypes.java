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
public enum LinkTypes {
    EN_CVPIPE(0, "Pipe with Check Valve", ""), //
    EN_PIPE(1, "Pipe", ""), //
    EN_PUMP(2, "Pump", ""), //
    EN_PRV(3, "Pressure Reducing Valve", "Pressure, psi (m)"), //
    EN_PSV(4, "Pressure Sustaining Valve", "Pressure, psi (m)"), //
    EN_PBV(5, "Pressure Breaker Valve", "Pressure, psi (m)"), //
    EN_FCV(6, "Flow Control Valve", "Flow (flow unit)"), //
    EN_TCV(7, "Throttle Control Valve", "Loss Coefficient"), //
    EN_GPV(8, "General Purpose Valve", "ID of head loss curve");

    private int code;
    private String description;
    private String setting;
    LinkTypes( int code, String description , String setting) {
        this.code = code;
        this.description = description;
        this.setting = setting;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
    
    public String getSetting() {
        return setting;
    }

    public static LinkTypes forCode( int i ) {
        LinkTypes[] values = values();
        for( LinkTypes type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }
}
