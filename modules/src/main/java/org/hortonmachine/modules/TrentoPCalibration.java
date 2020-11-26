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

import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_KEYWORDS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_LABEL;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_LICENSE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_NAME;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_STATUS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_dt_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_inRain_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_outDischarge_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_outFillDegree_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_outPipes_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_outTpMax_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_pOutPipe_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_tMax_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_tpMaxCalibration_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TMAX;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;
import org.joda.time.DateTime;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
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
public class TrentoPCalibration extends HMModel {

    @Description(OMSTRENTOP_pOutPipe_DESCRIPTION)
    @Unit("-")
    @In
    public Integer pOutPipe = null;

    @Description(OMSTRENTOP_dt_DESCRIPTION)
    @Unit("minutes")
    @In
    public Integer dt;

    @Description(OMSTRENTOP_tMax_DESCRIPTION)
    @Unit("-")
    @In
    //maximum duration of the current calibration simulation
    public int tMax = (int) DEFAULT_TMAX;

    @Description(OMSTRENTOP_tpMaxCalibration_DESCRIPTION)
    @Unit("minutes")
    @In
    //overall duration of the rainfall used for the calibration if statistical model is used 
    public Integer tpMaxCalibration = null;

    @Description(OMSTRENTOP_inRain_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inRain = null;

    @Description("The TrentoP project data folder.")
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder = null;

    @Description(OMSTRENTOP_outPipes_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPipes = null;

    @Description(OMSTRENTOP_outDischarge_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDischarge;

    @Description(OMSTRENTOP_outFillDegree_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFillDegree;

    @Description(OMSTRENTOP_outTpMax_DESCRIPTION)
    @Unit("minutes")
    @Out
    public Integer outTpMax = null;

    private double[][] results;

    @Execute
    public void process() throws Exception {

        checkNull(inFolder);

        File baseFolderFile = new File(inFolder);
        File pipesFile = new File(baseFolderFile, Constants.NETWORK_CALIBRATION_NAME_SHP);
        File areasFile = new File(baseFolderFile, Constants.AREA_NAME_SHP);
        File junctionsFile = new File(baseFolderFile, Constants.JUNCTIONS_NAME_SHP);
        File parametersFile = new File(baseFolderFile, Constants.PARAMETERS_CSV);

        checkFileExists(pipesFile.getAbsolutePath(), areasFile.getAbsolutePath(), junctionsFile.getAbsolutePath(),
                parametersFile.getAbsolutePath());
        OmsTrentoPCalibration trento_P = new OmsTrentoPCalibration();
        trento_P.pOutPipe = pOutPipe;
        if (inRain != null) {
            OmsTimeSeriesReader rainReader = new OmsTimeSeriesReader();
            rainReader.fileNovalue = "-9999";
            rainReader.file = inRain;
            rainReader.read();
            rainReader.close();
            trento_P.inRain = rainReader.outData;
        }

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

        trento_P.dt = dt;
        trento_P.tMax = tMax;
        trento_P.tpMaxCalibration = tpMaxCalibration;
        
        
        trento_P.process();

        results = trento_P.getResults();

        if (outDischarge != null && trento_P.outDischarge != null) {
            double[][] outDisch = hashToMatrix(trento_P.outDischarge, results.length);
            StringBuilder sb = printMatrixData(outDisch);
            FileUtilities.writeFile(sb.toString(), new File(outDischarge));
        }
        if (outFillDegree != null && trento_P.outFillDegree != null) {
            double[][] outFill = hashToMatrix(trento_P.outFillDegree, results.length);
            StringBuilder sb = printMatrixData(outFill);
            FileUtilities.writeFile(sb.toString(), new File(outFillDegree));
        }

    }

    public double[][] getResults() {
        return results;
    }

    private StringBuilder printMatrixData( double[][] matrix ) {
        StringBuilder sb = new StringBuilder();
        int cols = matrix[0].length;
        int rows = matrix.length;
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                sb.append(matrix[r][c]);
                sb.append(", ");
            }
            sb.append("\n");
        }
        return sb;
    }

    private double[][] hashToMatrix( HashMap<DateTime, HashMap<Integer, double[]>> outDischarge, int nStation ) {
        // create the rains array from the input.
        Set<Entry<DateTime, HashMap<Integer, double[]>>> dischargeSet = outDischarge.entrySet();
        DateTime first = null;
        DateTime second = null;
        int l = outDischarge.size();

        double[][] rainData = new double[l][nStation + 1];
        int index = 0;
        int dt = 0;
        for( Entry<DateTime, HashMap<Integer, double[]>> dischargeRecord : dischargeSet ) {

            DateTime dateTime = dischargeRecord.getKey();
            HashMap<Integer, double[]> values = dischargeRecord.getValue();
            if (first == null) {
                first = dateTime;
                rainData[index][0] = 1;
                Set<Integer> tmp = values.keySet();
                int i = 0;
                for( Integer f : tmp ) {
                    rainData[index][i + 1] = values.get(f)[0];
                    i++;
                }

            } else if (second == null) {
                second = dateTime;
                dt = Math.abs(second.getMinuteOfDay() - first.getMinuteOfDay());
                rainData[index][0] = rainData[index - 1][0] + dt;
                Set<Integer> tmp = values.keySet();
                int i = 0;
                for( Integer f : tmp ) {
                    rainData[index][i + 1] = values.get(f)[0];
                    i++;
                }

            } else {
                rainData[index][0] = rainData[index - 1][0] + dt;
                int i = 0;
                Set<Integer> tmp = values.keySet();
                for( Integer f : tmp ) {
                    rainData[index][i + 1] = values.get(f)[0];
                    i++;
                }
            }
            index++;
        }
        return rainData;
    }

    public static void main( String[] args ) throws Exception {
        String outFolder = "D:\\Dropbox\\hydrologis\\lavori\\2020_10_trentop\\test_soraga\\new2\\";
//        String outFolder = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_10_trentop/test_soraga/new/";
        TrentoPCalibration c = new TrentoPCalibration();
        c.pOutPipe = 10;
        c.dt = 5;
        c.tMax = 90;
        c.tpMaxCalibration = 60;
        c.inFolder = outFolder;
        c.outDischarge = outFolder + "outdischarge.csv";
        c.outFillDegree = outFolder + "outfilldegree.csv";
        c.process();

    }

}
