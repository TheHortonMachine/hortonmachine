/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
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
package org.hortonmachine.modules;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.labeler.OmsLabeler;

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

@Description("Connected components labeling operation")
@Author(name = "Simon Horne, Andrea Antonello", contact = "http://homepages.inf.ed.ac.uk/rbf/HIPR2/, www.hydrologis.com")
@Keywords("Labeling, Raster")
@Label(HMConstants.RASTERPROCESSING)
@Name("labeler")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Labeler extends OmsLabeler {

    @Description("The map to label.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inMap = null;

    @Description("The resulting map.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsLabeler labeler = new OmsLabeler();
        labeler.inMap = getRaster(inMap);
        labeler.pm = pm;
        labeler.process();
        dumpRaster(labeler.outMap, outMap);
    }
}
