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
public enum TimeParameterCodesStatistic {
    STATISTIC_NONE(0, "NONE", ""), //
    STATISTIC_AVERAGE(1, "AVERAGED", ""), //
    STATISTIC_MINIMUM(2, "MINIMUMS", ""), //
    STATISTIC_MAXIMUM(3, "MAXIMUMS", ""), //
    STATISTIC_RANGE(4, "RANGES", "");

    private int code;
    private String key;
    private String description;
    TimeParameterCodesStatistic( int code, String key, String description ) {
        this.code = code;
        this.key = key;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public static TimeParameterCodesStatistic forCode( int i ) {
        TimeParameterCodesStatistic[] values = values();
        for( TimeParameterCodesStatistic type : values ) {
            if (type.code == i) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given code: " + i);
    }

    public static TimeParameterCodesStatistic forKey( String key ) {
        TimeParameterCodesStatistic[] values = values();
        for( TimeParameterCodesStatistic type : values ) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No type for the given key: " + key);
    }
}
