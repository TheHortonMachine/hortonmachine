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

import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_KEYWORDS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_LABEL;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_LICENSE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_NAME;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_STATUS;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_dt_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_inDiameters_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_inPipes_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_inRain_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_outDischarge_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_outFillDegree_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_outPipes_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_outTpMax_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pA_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pAccuracy_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pAlign_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pC_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pCelerityFactor_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pEpsilon_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pEspInflux_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pExponent_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pFranco_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pG_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pGamma_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pJMax_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pMaxJunction_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pMaxTheta_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pMinDischarge_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pMinG_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pMinimumDepth_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pMode_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pN_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pOutPipe_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pTau_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_pTolerance_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_tDTp_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_tMax_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_tpMaxCalibration_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_tpMax_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP.OMSTRENTOP_tpMin_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_ACCURACY;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_CELERITY_FACTOR;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_EPSILON;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_ESP1;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_EXPONENT;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_FRANCO;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_GAMMA;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_J_MAX;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MAX_JUNCTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MAX_THETA;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MING;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MINIMUM_DEPTH;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MIN_DISCHARGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TDTP;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TMAX;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TOLERANCE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TPMIN;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.DiametersReader;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Range;
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
public class TrentoP extends HMModel {

	@Description(OMSTRENTOP_pMode_DESCRIPTION)
	@In
	public int pMode;

	@Description(OMSTRENTOP_pMinimumDepth_DESCRIPTION)
	@Unit("m")
	@In
	public double pMinimumDepth = DEFAULT_MINIMUM_DEPTH;

	@Description(OMSTRENTOP_pMaxJunction_DESCRIPTION)
	@Unit("-")
	@Range(max = 6, min = 0)
	@In
	public int pMaxJunction = DEFAULT_MAX_JUNCTION;

	@Description(OMSTRENTOP_pJMax_DESCRIPTION)
	@Unit("-")
	@Range(max = 1000, min = 3)
	@In
	public int pJMax = DEFAULT_J_MAX;

	@Description(OMSTRENTOP_pAccuracy_DESCRIPTION)
	@Unit("-")
	@Range(min = 0)
	@In
	public Double pAccuracy = DEFAULT_ACCURACY;

	@Description(OMSTRENTOP_pEpsilon_DESCRIPTION)
	@Unit("-")
	@Range(max = 1, min = 0)
	@In
	public double pEpsilon = DEFAULT_EPSILON;

	@Description(OMSTRENTOP_pMinG_DESCRIPTION)
	@Unit("-")
	@Range(max = 0.1, min = 0)
	@In
	public double pMinG = DEFAULT_MING;

	@Description(OMSTRENTOP_pMinDischarge_DESCRIPTION)
	@Unit("m3/s")
	@Range(min = 0)
	@In
	public double pMinDischarge = DEFAULT_MIN_DISCHARGE;

	@Description(OMSTRENTOP_pMaxTheta_DESCRIPTION)
	@Unit("-")
	@Range(min = Math.PI)
	@In
	public double pMaxTheta = DEFAULT_MAX_THETA;

	@Description(OMSTRENTOP_pCelerityFactor_DESCRIPTION)
	@Unit("-")
	@Range(min = 1, max = 1.6)
	@In
	public double pCelerityFactor = DEFAULT_CELERITY_FACTOR;

	@Description(OMSTRENTOP_pExponent_DESCRIPTION)
	@Unit("-")
	@Range(min = 0)
	@In
	public double pExponent = DEFAULT_EXPONENT;

	@Description(OMSTRENTOP_pTolerance_DESCRIPTION)
	@Unit("-")
	@Range(min = 0)
	@In
	public double pTolerance = DEFAULT_TOLERANCE;

	@Description(OMSTRENTOP_pC_DESCRIPTION)
	@Unit("-")
	@Range(min = 0)
	@In
	public double pC = 1;

	@Description(OMSTRENTOP_pGamma_DESCRIPTION)
	@Unit("-")
	@Range(min = 0)
	@In
	public double pGamma = DEFAULT_GAMMA;

	@Description(OMSTRENTOP_pEspInflux_DESCRIPTION)
	@Unit("-")
	@Range(min = 0)
	@In
	public double pEspInflux = DEFAULT_ESP1;

	@Description(OMSTRENTOP_pFranco_DESCRIPTION)
	@Unit("m")
	@Range(min = 0)
	@In
	public double pFranco = DEFAULT_FRANCO;

	@Description(OMSTRENTOP_pA_DESCRIPTION)
	@Unit("-")
	@Range(min = 0)
	@In
	public Double pA;

	@Description(OMSTRENTOP_pN_DESCRIPTION)
	@Unit("-")
	@Range(min = 0.05, max = 0.95)
	@In
	public Double pN;

	@Description(OMSTRENTOP_pTau_DESCRIPTION)
	@Unit("N/m2")
	@Range(min = 0)
	@In
	public Double pTau;

	@Description(OMSTRENTOP_pG_DESCRIPTION)
	@Unit("-")
	@Range(min = 0, max = 0.99)
	@In
	public Double pG;

	@Description(OMSTRENTOP_pAlign_DESCRIPTION)
	@In
	public Integer pAlign;

	@Description(OMSTRENTOP_inDiameters_DESCRIPTION)
	@UI(HMConstants.FILEIN_UI_HINT_CSV)
	@In
	public String inDiameters;

	@Description(OMSTRENTOP_pOutPipe_DESCRIPTION)
	@Unit("-")
	@In
	public Integer pOutPipe = null;

	@Description(OMSTRENTOP_tDTp_DESCRIPTION)
	@Unit("-")
	@Range(min = 0.015)
	@In
	public double tDTp = DEFAULT_TDTP;

	@Description(OMSTRENTOP_tpMin_DESCRIPTION)
	@Unit("-")
	@Range(min = 5)
	@In
	public double tpMin = DEFAULT_TPMIN;

	@Description(OMSTRENTOP_tpMax_DESCRIPTION)
	@Unit("minutes")
	@Range(min = 30)
	@In
	public double tpMax = DEFAULT_TMAX;

	@Description(OMSTRENTOP_tMax_DESCRIPTION)
	@Unit("-")
	@In
	public int tMax = (int) DEFAULT_TMAX;

	@Description(OMSTRENTOP_tpMaxCalibration_DESCRIPTION)
	@Unit("minutes")
	@In
	public Integer tpMaxCalibration = null;

	@Description(OMSTRENTOP_dt_DESCRIPTION)
	@Unit("minutes")
	@In
	public Integer dt;

	@Description(OMSTRENTOP_inRain_DESCRIPTION)
	@UI(HMConstants.FILEIN_UI_HINT_CSV)
	@In
	public String inRain = null;

	@Description(OMSTRENTOP_inPipes_DESCRIPTION)
	@UI(HMConstants.FILEIN_UI_HINT_VECTOR)
	@In
	public String inPipes = null;

	@Description(OMSTRENTOP_outPipes_DESCRIPTION)
	@UI(HMConstants.FILEOUT_UI_HINT)
	@Out
	public String outPipes = null;

	@Description(OMSTRENTOP_outDischarge_DESCRIPTION)
	@UI(HMConstants.FILEOUT_UI_HINT)
	@Out
	public String outDischarge;

	@Description(OMSTRENTOP_outFillDegree_DESCRIPTION)
	@UI(HMConstants.FILEOUT_UI_HINT)
	@Out
	public String outFillDegree;

	@Description(OMSTRENTOP_outTpMax_DESCRIPTION)
	@Unit("minutes")
	@Out
	public Integer outTpMax = null;

	public static String dateTimeFormatterYYYYMMDDHHMMSS_string = "yyyy-MM-dd HH:mm:ss";
	public static DateTimeFormatter tf = DateTimeFormat.forPattern(dateTimeFormatterYYYYMMDDHHMMSS_string);
	private double[][] results;

	@Execute
	public void process() throws Exception {
		OmsTrentoP trento_P = new OmsTrentoP();

		// set parameters;
		trento_P.pMode = pMode;
		trento_P.pA = pA;
		trento_P.pN = pN;
		trento_P.pTau = pTau;
		trento_P.pG = pG;
		trento_P.pAlign = pAlign;
		trento_P.pMinimumDepth = pMinimumDepth;
		trento_P.pMaxJunction = pMaxJunction;
		trento_P.pJMax = pJMax;
		trento_P.pAccuracy = pAccuracy;
		trento_P.tDTp = tDTp;
		trento_P.dt = dt;
		trento_P.tMax = tMax;
		trento_P.tpMin = tpMin;
		trento_P.tpMax = tpMax;
		trento_P.pEpsilon = pEpsilon;
		trento_P.pMinG = pMinG;
		trento_P.pMinDischarge = pMinDischarge;
		trento_P.pMaxTheta = pMaxTheta;
		trento_P.pCelerityFactor = pCelerityFactor;
		trento_P.pExponent = pExponent;
		trento_P.pTolerance = pTolerance;
		trento_P.pC = pC;
		trento_P.pGamma = pGamma;
		trento_P.pEspInflux = pEspInflux;
		if (inDiameters != null) {
			DiametersReader diametersreader = new DiametersReader();
			diametersreader.file = inDiameters;
			diametersreader.pCols = 2;
			diametersreader.pSeparator = "\\s+";
			diametersreader.fileNovalue = "-9999.0";
			diametersreader.readFile();
			trento_P.inDiameters = diametersreader.data;
		}
		trento_P.pOutPipe = pOutPipe;
		if (inRain != null) {
			OmsTimeSeriesReader rainReader = new OmsTimeSeriesReader();
			rainReader.fileNovalue = "-9999";
			rainReader.file = inRain;
			rainReader.read();
			rainReader.close();
			trento_P.inRain = rainReader.outData;
		}
		trento_P.inPipes = getVector(inPipes);

		trento_P.process();

		dumpVector(trento_P.outPipes, outPipes);
		results = trento_P.getResults();

		if (outDischarge != null && trento_P.outDischarge != null) {
			timehashToMatrix(new File(outDischarge), trento_P.outDischarge);
//			double[][] outDisch = hashToMatrix(trento_P.outDischarge, results.length);
//			StringBuilder sb = printMatrixData(outDisch);
//			FileUtilities.writeFile(sb.toString(), new File(outDischarge));
		}
		if (outFillDegree != null && trento_P.outFillDegree != null) {
			timehashToMatrix(new File(outFillDegree), trento_P.outFillDegree);
//			double[][] outFill = hashToMatrix(trento_P.outFillDegree, results.length);
//			StringBuilder sb = printMatrixData(outFill);
//			FileUtilities.writeFile(sb.toString(), new File(outFillDegree));
		}

	}

	public double[][] getResults() {
		return results;
	}

	private StringBuilder printMatrixData(double[][] matrix) {
		StringBuilder sb = new StringBuilder();
		int cols = matrix[0].length;
		int rows = matrix.length;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				sb.append(matrix[r][c]);
				sb.append(", ");
			}
			sb.append("\n");
		}
		return sb;
	}

	private double[][] hashToMatrix(HashMap<DateTime, HashMap<Integer, double[]>> outDischarge, int nStation) {
		// create the rains array from the input.
		Set<Entry<DateTime, HashMap<Integer, double[]>>> dischargeSet = outDischarge.entrySet();
		DateTime first = null;
		DateTime second = null;
		int l = outDischarge.size();

		double[][] rainData = new double[l][nStation + 1];
		int index = 0;
		int dt = 0;
		for (Entry<DateTime, HashMap<Integer, double[]>> dischargeRecord : dischargeSet) {

			DateTime dateTime = dischargeRecord.getKey();
			HashMap<Integer, double[]> values = dischargeRecord.getValue();
			if (first == null) {
				first = dateTime;
				rainData[index][0] = 1;
				Set<Integer> tmp = values.keySet();
				int i = 0;
				for (Integer f : tmp) {
					rainData[index][i + 1] = values.get(f)[0];
					i++;
				}

			} else if (second == null) {
				second = dateTime;
				dt = Math.abs(second.getMinuteOfDay() - first.getMinuteOfDay());
				rainData[index][0] = rainData[index - 1][0] + dt;
				Set<Integer> tmp = values.keySet();
				int i = 0;
				for (Integer f : tmp) {
					rainData[index][i + 1] = values.get(f)[0];
					i++;
				}

			} else {
				rainData[index][0] = rainData[index - 1][0] + dt;
				int i = 0;
				Set<Integer> tmp = values.keySet();
				for (Integer f : tmp) {
					rainData[index][i + 1] = values.get(f)[0];
					i++;
				}
			}
			index++;
		}
		return rainData;
	}

	private void timehashToMatrix(File outFile, HashMap<DateTime, HashMap<Integer, double[]>> valuesMap) throws Exception {

		String SEP = ";";
		StringBuilder header = new StringBuilder();
		StringBuilder data = new StringBuilder();

		for (Entry<DateTime, HashMap<Integer, double[]>> timeEntry : valuesMap.entrySet()) {
			DateTime dt = timeEntry.getKey();
			String dtStr = dt.toString(tf);
			data.append(dtStr);
			HashMap<Integer, double[]> pipesEntry = timeEntry.getValue();
			boolean doHeader = header.length() == 0;
			for (Entry<Integer, double[]> ds : pipesEntry.entrySet()) {
				int pipeId = ds.getKey();
				double value = ds.getValue()[0];
				if (doHeader) {
					header.append(SEP).append(pipeId);
				}
				data.append(SEP).append(value);
			}
			data.append("\n");
		}
		header.append("\n");
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
			bw.write("timestep"+header);
			bw.write(data.toString());
		}

	}

}
