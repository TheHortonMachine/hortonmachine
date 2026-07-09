/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) G-ANT - www.g-ant.eu 
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
package org.hortonmachine.gears.utils.crs;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.proj.PROJFormattable;
import org.geotools.referencing.proj.PROJFormatter;
import org.locationtech.jts.geom.Envelope;

/**
 * A class to hold projection information and export it as JSON.
 * This returns a proj definition where possible.
 * 
 * @author Andrea Antonello
 */
public class ProjectionInfo {

	private String epsgCode;
	private String name;
	private String axisOrder;
	private String units;
	private String proj4;
	private String wkt;
	private Envelope extent;
	private Envelope extent4326;
	
	private boolean initialized = false;

	public ProjectionInfo(String epsgCode) {
		if (epsgCode == null || epsgCode.trim().isEmpty()) {
			throw new IllegalArgumentException("EPSG code cannot be null or empty");
		}
		this.epsgCode = epsgCode;
	}
	
	public String getEpsgCode() {
		return epsgCode;
	}
	
	public String getName() throws Exception {
		init();
		return name;
	}
	
	public String getAxisOrder() throws Exception {
		init();
		return axisOrder;
	}
	
	public String getUnits() throws Exception {
		init();
		return units;
	}
	
	public String getProj4() throws Exception {
		init();
		return proj4;
	}
	
	public String getWkt() throws Exception {
		init();
		return wkt;
	}
	
	public Envelope getExtent() throws Exception {
		init();
		return extent;
	}
	
	public Envelope getExtent4326() throws Exception {
		init();
		return extent4326;
	}
	
	public String toString() {
		try {
			return toJson();
		} catch (Exception e) {
			return "ProjectionInfo{epsgCode='" + epsgCode + "', error='" + e.getMessage() + "'}";
		}
	}
	
	public String toJson() throws Exception {
		init();
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		field(sb, "code", epsgCode, true);
		field(sb, "name", name, true);
		field(sb, "proj4", proj4, true);
		field(sb, "units", units, true);
		field(sb, "axisOrder", axisOrder, true);
		if (extent != null) {
			field(sb, "extent", String.format("[%f, %f, %f, %f]", extent.getMinX(), extent.getMinY(), extent.getMaxX(), extent.getMaxY()), true);
		} else {
			field(sb, "extent", null, true);
		}
		if (extent4326 != null) {
			field(sb, "extent4326", String.format("[%f, %f, %f, %f]", extent4326.getMinX(), extent4326.getMinY(), extent4326.getMaxX(), extent4326.getMaxY()), true);
		} else {
			field(sb, "extent4326", null, true);
		}
		field(sb, "wkt", wkt, false);
		sb.append("}");
		return sb.toString();
	}
	
    private static void field(StringBuilder sb, String key, String value, boolean comma) {
        sb.append("  \"").append(escape(key)).append("\": ");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append("\"").append(escape(value)).append("\"");
        }
        if (comma) {
            sb.append(",");
        }
        sb.append("\n");
    }
    
    private static String escape(String s) {
        return s
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }

	private void init() throws Exception {
		if (initialized) {
			return;
		}
		CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs(epsgCode, true);

		name = crs.getName() != null ? crs.getName().getCode() : null;
		axisOrder = CRS.getAxisOrder(crs).name();
		units = null;
		proj4 = null;
		wkt = null;

		try {
			units = crs.getCoordinateSystem().getAxis(0).getUnit().toString();
		} catch (Exception e) {
			units = null;
		}

		try {
			if (crs instanceof PROJFormattable) {
				proj4 = new PROJFormatter().toPROJ((PROJFormattable) crs);
			}
		} catch (Exception e) {
			proj4 = null;
		}

		try {
			ReferencedEnvelope env = new ReferencedEnvelope(CRS.getEnvelope(crs));
			if (!env.isEmpty()) {
				extent = new Envelope(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY());
				extent4326  = env.transform(CrsUtilities.WGS84, true);
			}
		} catch (Exception e) {
			extent = null;
		}

		try {
			wkt = crs.toWKT();
		} catch (Exception e) {
			wkt = null;
		}
	}
}
