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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_IN_RASTER1_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_IN_RASTER2_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_KEYWORDS;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_LABEL;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_LICENSE;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_NAME;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses.OMSRASTERTRANSITIONCLASSES_STATUS;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses;
import org.hortonmachine.gears.utils.files.FileUtilities;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSRASTERTRANSITIONCLASSES_DESCRIPTION)
@Documentation(OMSRASTERTRANSITIONCLASSES_DOCUMENTATION)
@Author(name = OMSRASTERTRANSITIONCLASSES_AUTHORNAMES, contact = OMSRASTERTRANSITIONCLASSES_AUTHORCONTACTS)
@Keywords(OMSRASTERTRANSITIONCLASSES_KEYWORDS)
@Label(OMSRASTERTRANSITIONCLASSES_LABEL)
@Name(OMSRASTERTRANSITIONCLASSES_NAME)
@Status(OMSRASTERTRANSITIONCLASSES_STATUS)
@License(OMSRASTERTRANSITIONCLASSES_LICENSE)
public class RasterTransitionClasses extends HMModel {

    @Description(OMSRASTERTRANSITIONCLASSES_IN_RASTER1_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inPreviousRaster;

    @Description(OMSRASTERTRANSITIONCLASSES_IN_RASTER2_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNextRaster;

    @Description(OMSRASTERTRANSITIONCLASSES_OUT_RASTER_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Description("The output csv file of transition classes")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTransition;

    @Execute
    public void process() throws Exception {

        OmsRasterTransitionClasses r = new OmsRasterTransitionClasses();
        r.inPreviousRaster = getRaster(inPreviousRaster);
        r.inNextRaster = getRaster(inNextRaster);
        r.process();
        dumpRaster(r.outRaster, outRaster);

        StringBuilder sb = new StringBuilder();
        sb.append("crossclasscode; fromclass; toclass; count\n");

        Map<Integer, String> newClasses2KeyMap = r.key2NewClassesMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        for( Entry<Integer, Integer> entry : r.newClasses2CountMap.entrySet() ) {
            Integer newClass = entry.getKey();
            Integer count = entry.getValue();
            String transitionStr = newClasses2KeyMap.get(newClass);
            String[] split = transitionStr.split(" ");
            String from = split[0];
            String to = split[1];

            sb.append(newClass).append(";").append(from).append(";").append(to).append(";").append(count).append("\n");
        }

        FileUtilities.writeFile(sb.toString(), new File(outTransition));

    }

}
