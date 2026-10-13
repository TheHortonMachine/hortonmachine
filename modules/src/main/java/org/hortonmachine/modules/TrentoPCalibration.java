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
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_DOCUMENTATION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_KEYWORDS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_LABEL;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_LICENSE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_NAME;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_STATUS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_dt_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_inRain_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_outDischarge_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_outFillDegree_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_outTpMax_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_pOutPipe_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_tMax_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration.OMSTRENTOP_tpMaxCalibration_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPProject.BIBLIOGRAPHY_RODRIGUEZ_ITURBE_VALDES_1979;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoPCalibration;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPDatabase;
import org.joda.time.DateTime;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
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
@Documentation(OMSTRENTOP_DOCUMENTATION)
@Bibliography({BIBLIOGRAPHY_RODRIGUEZ_ITURBE_VALDES_1979})
@Author(name = OMSTRENTOP_AUTHORNAMES, contact = OMSTRENTOP_AUTHORCONTACTS)
@Keywords(OMSTRENTOP_KEYWORDS)
@Label(OMSTRENTOP_LABEL)
@Name(OMSTRENTOP_NAME)
@Status(OMSTRENTOP_STATUS)
@License(OMSTRENTOP_LICENSE)
public class TrentoPCalibration extends HMModel {

    @Description("The database of the network, with the layers network_calibration, areas and junctions and the table parameters: a GeoPackage (.gpkg) or H2GIS (.mv.db) file, or a PostGIS database, as jdbc:postgresql://host:port/database. The verified network is written into it, as the layer network_calibration_output, with the maximum discharge (qmax, l/s), its time (t_qmax, minutes) and the maximum fill degree (fillmax) of each pipe.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inDatabase = null;

    @Description("The optional user of the database.")
    @In
    public String pUser = null;

    @Description("The optional password of the database.")
    @In
    public String pPassword = null;

    @Description(OMSTRENTOP_pOutPipe_DESCRIPTION)
    @Unit("-")
    @In
    public Integer pOutPipe = null;

    @Description(OMSTRENTOP_dt_DESCRIPTION)
    @Unit("minutes")
    @In
    public Integer dt = null;

    @Description(OMSTRENTOP_tMax_DESCRIPTION)
    @Unit("minutes")
    @In
    public Integer tMax = null;

    @Description(OMSTRENTOP_tpMaxCalibration_DESCRIPTION)
    @Unit("minutes")
    @In
    public Integer tpMaxCalibration = null;

    @Description(OMSTRENTOP_inRain_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inRain = null;

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

        checkNull(inDatabase, pOutPipe);

        OmsTrentoPCalibration trento_P = new OmsTrentoPCalibration();
        trento_P.pm = pm;
        trento_P.pOutPipe = pOutPipe;
        if (inRain != null) {
            OmsTimeSeriesReader rainReader = new OmsTimeSeriesReader();
            rainReader.fileNovalue = "-9999";
            rainReader.file = inRain;
            rainReader.read();
            rainReader.close();
            trento_P.inRain = rainReader.outData;
        }

        try (ASpatialDb db = TrentoPDatabase.open(inDatabase, pUser, pPassword)) {
            trento_P.inParameters = TrentoPDatabase.readParameters(db);
            trento_P.inPipes = TrentoPDatabase.readLayer(db, Constants.NETWORK_CALIBRATION_LAYER);
            trento_P.inAreas = TrentoPDatabase.readLayer(db, Constants.AREAS_LAYER);
            trento_P.inJunctions = TrentoPDatabase.readLayer(db, Constants.JUNCTIONS_LAYER);

            trento_P.dt = dt;
            trento_P.tMax = tMax;
            trento_P.tpMaxCalibration = tpMaxCalibration;
            trento_P.process();

            TrentoPDatabase.writeLayer(db, trento_P.outPipes, Constants.NETWORK_CALIBRATION_OUTPUT_LAYER, pm);
        }

        results = trento_P.getResults();
        outTpMax = trento_P.outTpMax;

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
                if (c > 0) {
                    sb.append(", ");
                }
                if (r == 0) {
                    if (c == 0) {
                        sb.append("TIME");
                    } else {
                        sb.append((int) matrix[r][c]);
                    }
                } else {
                    sb.append(matrix[r][c]);
                }
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

        double[][] rainData = new double[l+1][nStation + 1];
        int index = 0;
        int dt = 0;
        for( Entry<DateTime, HashMap<Integer, double[]>> dischargeRecord : dischargeSet ) {

            DateTime dateTime = dischargeRecord.getKey();
            HashMap<Integer, double[]> values = dischargeRecord.getValue();
            if (first == null) {
                first = dateTime;
                Set<Integer> tmp = values.keySet();
                int i = 0;
                // create header
                rainData[index][0] = -1;
                for( Integer f : tmp ) {
                    rainData[index][i + 1] = f;
                    i++;
                }
                index++;
                // first line of data
                rainData[index][0] = 0;
                i = 0;
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

}
