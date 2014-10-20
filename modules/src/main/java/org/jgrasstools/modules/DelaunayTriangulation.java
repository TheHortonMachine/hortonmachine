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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_CERTIFIED;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.jgrasstools.gears.modules.v.triangulation.OmsDelaunayTriangulation.fElev_DESCR;
import static org.jgrasstools.gears.modules.v.triangulation.OmsDelaunayTriangulation.inMap_DESCR;
import static org.jgrasstools.gears.modules.v.triangulation.OmsDelaunayTriangulation.outMap_DESCR;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.modules.v.triangulation.OmsDelaunayTriangulation;

@Description(OmsDelaunayTriangulation.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsDelaunayTriangulation.KEYWORDS)
@Label(JGTConstants.VECTORPROCESSING)
@Name(OmsDelaunayTriangulation.NAME)
@Status(OMSHYDRO_CERTIFIED)
@License(OMSHYDRO_LICENSE)
public class DelaunayTriangulation extends JGTModel {

    @Description(inMap_DESCR)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inMap = null;

    @Description(fElev_DESCR)
    @In
    public String fElev = "elev";

    @Description(outMap_DESCR)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsDelaunayTriangulation delaunayTriangulation = new OmsDelaunayTriangulation();
        delaunayTriangulation.pm = pm;
        delaunayTriangulation.inMap = getVector(inMap);
        delaunayTriangulation.fElev = fElev;
        delaunayTriangulation.process();
        dumpVector(delaunayTriangulation.outMap, outMap);
    }

}
