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
package org.jgrasstools.modules;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Name;

import org.jgrasstools.hortonmachine.modules.geomorphology.geomorphon.OmsGeomorphon;

@Name("geomorphonraster")
public class Geomorphon extends OmsGeomorphon {
    @Description("An elevation raster.")
    @In
    public String inElev;

    @Description("Output categories raster.")
    @In
    public String outRaster;

    public void process() throws Exception {
        OmsGeomorphon geomorphon = new OmsGeomorphon();
        geomorphon.inElev = getRaster(inElev);
        geomorphon.pRadius = pRadius;
        geomorphon.pThreshold = pThreshold;
        geomorphon.pm = pm;
        geomorphon.process();
        dumpRaster(geomorphon.outRaster, outRaster);
    }

}
