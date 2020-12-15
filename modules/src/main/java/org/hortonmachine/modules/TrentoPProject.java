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

import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_KEYWORDS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_LABEL;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_LICENSE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_NAME;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_STATUS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.OMSTRENTOP_pOutPipe_DESCRIPTION;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;

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
import oms3.annotations.Unit;

@Description(OMSTRENTOP_DESCRIPTION)
@Author(name = OMSTRENTOP_AUTHORNAMES, contact = OMSTRENTOP_AUTHORCONTACTS)
@Keywords(OMSTRENTOP_KEYWORDS)
@Label(OMSTRENTOP_LABEL)
@Name(OMSTRENTOP_NAME)
@Status(OMSTRENTOP_STATUS)
@License(OMSTRENTOP_LICENSE)
public class TrentoPProject extends HMModel {

    @Description(OMSTRENTOP_pOutPipe_DESCRIPTION)
    @Unit("-")
    @In
    public Integer pOutPipe = null;

    @Description("The TrentoP project data folder.")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder = null;

    private double[][] results;

    @Execute
    public void process() throws Exception {

        checkNull(inFolder);

        File baseFolderFile = new File(inFolder);
        File pipesFile = new File(baseFolderFile, Constants.NETWORK_PROJECT_NAME_SHP);
        File pipesOutFile = new File(baseFolderFile, Constants.NETWORK_PROJECT_OUTPUT_NAME_SHP);
        File areasFile = new File(baseFolderFile, Constants.AREA_NAME_SHP);
        File junctionsFile = new File(baseFolderFile, Constants.JUNCTIONS_NAME_SHP);
        File parametersFile = new File(baseFolderFile, Constants.PARAMETERS_CSV);
        File diametersFile = new File(baseFolderFile, Constants.DIAMETERS_CSV);

        checkFileExists(pipesFile.getAbsolutePath(), areasFile.getAbsolutePath(), junctionsFile.getAbsolutePath(),
                parametersFile.getAbsolutePath(), diametersFile.getAbsolutePath());

        OmsTrentoPProject trento_P = new OmsTrentoPProject();
        trento_P.pOutPipe = pOutPipe;

        trento_P.inDiameters = getDiameters(diametersFile);

        List<String> paramsList = FileUtilities.readFileToLinesList(parametersFile);
        paramsList.remove(0);
        HashMap<String, Number> paramsMap = new HashMap<String, Number>();
        for( String paramLine : paramsList ) {
            String[] lineSplit = paramLine.split(";");
            paramsMap.put(lineSplit[0], Double.parseDouble(lineSplit[1]));
        }

        trento_P.inParameters = paramsMap;

        trento_P.inPipes = getVector(pipesFile.getAbsolutePath());
        trento_P.inAreas = getVector(areasFile.getAbsolutePath());
        trento_P.inJunctions = getVector(junctionsFile.getAbsolutePath());

        trento_P.process();

        results = trento_P.getResults();

        SimpleFeatureCollection outPipes = trento_P.outPipes;
        dumpVector(outPipes, pipesOutFile.getAbsolutePath());
    }

    private List<double[]> getDiameters( File diametersFile ) throws IOException {
        List<String> linesList = FileUtilities.readFileToLinesList(diametersFile);

        List<double[]> diametersAndThicknessList = new ArrayList<double[]>();

        for( String line : linesList ) {
            line = line.trim();
            if (line.startsWith("#")) {
                continue;
            }
            String[] split = line.split(";");
            if (split.length != 3) {
                throw new ModelsIllegalargumentException(
                        "The diameters file has to contain lines with an ID, a diameter and a thickness per line, separated by semicolon.",
                        this);
            }
            double diameterId = Double.parseDouble(split[0]);
            double diameter = Double.parseDouble(split[1]);
            double thickness = Double.parseDouble(split[2]);

            diametersAndThicknessList.add(new double[]{diameter, thickness});
        }

        return diametersAndThicknessList;
    }

    public double[][] getResults() {
        return results;
    }

    public static void main( String[] args ) throws Exception {
        String outFolder = "D:\\lavori_tmp\\2020_trentoP\\project_03";
//        String outFolder = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_10_trentop/test_soraga/new/";
        TrentoPProject c = new TrentoPProject();
        c.pOutPipe = 10;
        c.inFolder = outFolder;
        c.process();

    }

}
