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
import java.io.IOException;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.modules.docker.GdalDockerModel;
import org.hortonmachine.modules.docker.PdalDockerModel;
import org.json.JSONArray;
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

@Description("PDAL filter.returns command.")
@Author(name = "Antonello Andrea", contact = "http://www.hydrologis.com")
@Keywords("gdal, docker")
@Label(HMConstants.GDAL)
@Name("_gdalinfo")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class PdalFilterReturns extends PdalDockerModel {
    @Description("The gdal file to check.")
    @UI(HMConstants.FILEIN_UI_HINT_LAS)
    @In
    public String inPath = null;

    @Description("The output file name.")
    @In
    public String outName = null;

    @Description("List of impulses to extract. Can be one or more of: first, last, intermediate, only")
    @In
    public String pGroups = "last, only";

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

                JSONObject filter = new JSONObject();
                filter.put("type", "filters.returns");
                filter.put("groups", pGroups);

                File pipelineFile = getPipelineFile(workspaceFile, inName, outName, filter);

                String cmd = "pdal pipeline " + pipelineFile.getName();

                startContainer(workspace);
                pm.beginTask("Running command...", -1);
                execCommand(cmd);
                pm.done();
            } finally {
                closeClient();
            }
        } else {
            pm.errorMessage(error);
        }
    }

    private File getPipelineFile( File workspaceFile, String inName, String outName, JSONObject filter ) throws IOException {
        JSONObject root = new JSONObject();
        JSONArray pipelineArray = new JSONArray();
        root.put("pipeline", pipelineArray);

        JSONObject reader = new JSONObject();
        reader.put("type", "readers.las");
        reader.put("filename", "./" + inName);
        pipelineArray.put(reader);

        pipelineArray.put(filter);

        JSONObject writer = new JSONObject();
        writer.put("type", "writers.las");
        writer.put("minor_version", 1);
        writer.put("filename", "./" + outName);
        pipelineArray.put(writer);

        String filterJson = root.toString(2);
        pm.message("Running pipeline with filter:");
        pm.message(filterJson);

        File pipeLineFile = new File(workspaceFile, "pipeline.json");
        FileUtilities.writeFile(filterJson, pipeLineFile);
        return pipeLineFile;
    }

    public static void main( String[] args ) throws Exception {
        PdalFilterReturns i = new PdalFilterReturns();
        i.inPath = "/Users/hydrologis/data/las/EXAMPLE_river.laz";
        i.outName = "filtered.las";
        i.process();
    }
}
