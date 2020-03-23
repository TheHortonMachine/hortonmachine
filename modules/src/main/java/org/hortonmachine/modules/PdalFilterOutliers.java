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

@Description("PDAL filter.outliers command: Extended Local Minimum")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("pdal, filter, outliers, docker")
@Label(HMConstants.PDAL)
@Name("_pdal_filter_outliers")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class PdalFilterOutliers extends PdalDockerModel {
    @Description("The pdal file to filter.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inPath = null;

    @Description("The output file name.")
    @In
    public String outName = null;

    @Description("The classification value to apply to outliers.")
    @In
    public Double pClass = 7.0;

    @Description("The outlier removal method (statistical or radius).")
    @In
    public String pMethod = "statistical";

    @Description("Minimum number of neighbors in radius (radius method only).")
    @In
    public Double pMinK = 2.0;

    @Description("Radius (radius method only).")
    @In
    public Double pRadius = 1.0;

    @Description("Mean number of neighbors (statistical method only).")
    @In
    public Double pMeanK = 8.0;

    @Description("Standard deviation threshold (statistical method only).")
    @In
    public Double pMultiplier = 2.0;

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
//                    "type": "filters.outlier",
//                                "method": "statistical",
//                                "multiplier": 2,
//                                "mean_k": 10
//                        }
                JSONObject filter = new JSONObject();
                filter.put("type", "filters.outlier");
                filter.put("method", pMethod);
                if (pClass != null) {
                    filter.put("class", pClass);
                }
                if (pMinK != null) {
                    filter.put("min_k", pMinK);
                }
                if (pMeanK != null) {
                    filter.put("mean_k", pMeanK);
                }
                if (pRadius != null) {
                    filter.put("radius", pRadius);
                }
                if (pMultiplier != null) {
                    filter.put("multiplier", pMultiplier);
                }

                String pipelineJson = getPipelineJson(inName, outName, filter);
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
        PdalFilterOutliers i = new PdalFilterOutliers();
        i.inPath = "/Users/hydrologis/data/las/EXAMPLE_river.las";
        i.pMethod = "statistical";
        i.pMultiplier = 2.0;
        i.pMeanK = 10.0;
        i.outName = "filtered.las";
        i.process();
    }
}
