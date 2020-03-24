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
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.modules.docker.PdalDockerModel;
import org.json.JSONObject;

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

@Description("PDAL filter.pmf command.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("pdal, filter, pmf, docker")
@Label(HMConstants.PDAL)
@Name("_pdal_filter_pmf")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class PdalFilterPmf extends PdalDockerModel {
    @Description("The pdal file to filter.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inPath = null;

    @Description("The output file name.")
    @In
    public String outName = null;

    @Description("Slope")
    @In
    public Double pSlope = 1.0;

    @Description("Use exponential growth for window sizes?")
    @In
    public boolean doExponential = true;

    @Description("Cell Size.")
    @In
    public Double pCellSize = 1.0;

    @Description("Maximum distance.")
    @In
    public Double pMaxDistance = 2.5;

    @Description("Maximum window size.")
    @In
    public Double pMaxWindowSize = 33.0;

    @Description("Initial distance.")
    @In
    public Double pInitialDistance = 0.15;

    @Description("An optional pre pmf input classification filter.")
    @In
    public Integer pInClassification = null;

    @Description("An optional post pmf output classification filter.")
    @In
    public Integer pOutClassification = null;

    @Execute
    public void process() throws Exception {
        checkFileExists(inPath);
        String error = checkDockerInstall();
        if (error == null) {
            try {
                File file = new File(inPath);
                String inName = file.getName();
                File workspaceFile = file.getParentFile();
                String workspace = workspaceFile.getAbsolutePath();

//                {
//                    "type":"filters.range",
//                    "limits":"Classification![7:7]"
//                },  
//                {
//                        "type":"filters.pmf",
//                        "slope": 1.2,
//                    "exponential": "true",
//                    "cell_size": 3.0,
//                    "max_distance": 1.2,
//                    "max_window_size": 20,
//                    "initial_distance": 0.15
//                    },
//                {
//                    "type":"filters.range",
//                    "limits":"Classification[2:2]"
//                },

                List<JSONObject> filters = new ArrayList<>();

                if (pInClassification != null) {
                    JSONObject inClassFilter = new JSONObject();
                    inClassFilter.put("type", "filters.range");
                    inClassFilter.put("limits", "Classification![" + pInClassification + ":" + pInClassification + "]");
                    filters.add(inClassFilter);
                }

                JSONObject filter = new JSONObject();
                filter.put("type", "filters.pmf");
                filter.put("exponential", doExponential);
                if (pSlope != null)
                    filter.put("slope", pSlope);
                if (pCellSize != null)
                    filter.put("cell_size", pCellSize);
                if (pMaxDistance != null)
                    filter.put("max_distance", pMaxDistance);
                if (pMaxWindowSize != null)
                    filter.put("max_window_size", pMaxWindowSize);
                if (pInitialDistance != null)
                    filter.put("initial_distance", pInitialDistance);
                filters.add(filter);

                if (pOutClassification != null) {
                    JSONObject outClassFilter = new JSONObject();
                    outClassFilter.put("type", "filters.range");
                    outClassFilter.put("limits", "Classification![" + pOutClassification + ":" + pOutClassification + "]");
                    filters.add(outClassFilter);
                }

                String pipelineJson = getPipelineJson(inName, outName,
                        (JSONObject[]) filters.toArray(new JSONObject[filters.size()]));
                
                pm.message("Running pipeline with filter:");
                pm.message(pipelineJson);
                File pipelineFile = getPipelineFile(workspaceFile, pipelineJson);

                String cmd = "pdal pipeline " + pipelineFile.getName();

                startContainer(workspace);
                pm.beginTask("Running command...", -1);
                execCommand(cmd);
                pm.done();

                pipelineFile.delete();
            } finally {
                closeClient();
            }
        } else {
            pm.errorMessage(error);
        }
    }

    public static void main( String[] args ) throws Exception {
        PdalFilterPmf i = new PdalFilterPmf();
        i.inPath = "/Users/hydrologis/data/las/EXAMPLE_river.las";
        i.outName = "filtered.las";
        i.process();
    }
}
