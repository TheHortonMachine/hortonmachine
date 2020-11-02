package org.hortonmachine.trentop.models;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

/**
 * The TrentoP model class for calibration processes.
 */
public class CalibrationModel {

    /**
     * The key for accuracy.
     */
    public static final String ACCURACY = "caccuracy"; //$NON-NLS-1$
    /**
     * The key for a.
     */
    public static final String A = "ca";//$NON-NLS-1$
    /**
     * The key for n.
     */
    public static final String N = "cn";//$NON-NLS-1$
    /**
     * The key for celerity.
     */
    public static final String CELERITY = "ccelerity";//$NON-NLS-1$
    /**
     * The key for esp.
     */
    public static final String EPS = "ceps";//$NON-NLS-1$
    /**
     * The key for exponent.
     */
    public static final String EXPONENT = "cexponent";//$NON-NLS-1$
    /**
     * The key for gamma.
     */
    public static final String GAMMA = "cgamma";//$NON-NLS-1$
    /**
     * The key for influx.
     */
    public static final String INFLUX = "cinflux";//$NON-NLS-1$
    /**
     * The key for jMax.
     */
    public static final String J_MAX = "cjMax";//$NON-NLS-1$
    /**
     * The key for maxFillDegree.
     */
    public static final String MAX_FILL_DEGREE = "cmaxFill";//$NON-NLS-1$
    /**
     * The key for tollerance.
     */
    public static final String TOLERANCE = "ctolerance";//$NON-NLS-1$
    /**
     * The key for maxJunction.
     */
    public static final String MAX_JUNCTION = "cmaxJunction";//$NON-NLS-1$
    /**
     * The key for minDischarge.
     */
    public static final String MIN_DISCHARGE = "cminDischarge";//$NON-NLS-1$
    /**
     * The key for minFillDegree.
     */
    public static final String MIN_FILL_DEGREE = "cminG";//$NON-NLS-1$
    /**
     * The key for maximum simulation time in calibration.
     */
    public static final String MAXIMUM_TIME = "cmaxT";//$NON-NLS-1$
    /**
     * The key for step time in calibration.
     */
    public static final String STEP = "cdt";//$NON-NLS-1$
    /**
     * The key for the output file.
     */
    public static final String OUT_DISCHARGE_FILE = "coutFile"; //$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#inputFile} which can be a diameters or rainData file..
     */
    public static final String INPUT_RAIN_FILE = "cinputFile"; //$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#inputFile} which is the fill degree..
     */
    public static final String OUT_FILL_FILE = "coutFillDegree"; //$NON-NLS-1$

    /**
     * A key to use get and set method in {@link PojoProperties#tpMaxCalibration} which is the time when the rain stops.
     */
    public static final String TP_MAX_CALIBRATION = "ctpMaxCalibration"; //$NON-NLS-1$
    /**
     * The key for the outlet of the geosewere network.
     */
    public static final String OUT_PIPE = "coutPipe"; //$NON-NLS-1$
    /*
     * where to store the discharge data in calibration mode.
     */
    private String outDischargeFile;
    /*
     * where to store the fill degree data in calibration mode.
     */
    private String outFillFile;

    /**
     * The warning that come out from the models.
     */
    private String warning = null;
    /*
     * The models parameters.
     */
    private OmsTrentoP trentoP;
    /*
     * Where to extract the rain data or diameters.
     */
    private String inputRainFile;

    public CalibrationModel() {
        trentoP = new OmsTrentoP();
    }

    public String getWarning() {
        return warning;
    }

    public String getCoutFile() {
        return outDischargeFile;
    }

    public void setCoutFile( String outFile ) {
        if (outFile != null) {
            PreferencesHandler.setPreference(OUT_DISCHARGE_FILE, outFile);
        }
        this.outDischargeFile = outFile;
    }

    public String getCoutFillDegree() {
        return outFillFile;
    }

    public void setCoutFillDegree( String outFillFile ) {
        if (outFillFile != null) {
            PreferencesHandler.setPreference(OUT_FILL_FILE, outFillFile);
        }
        this.outFillFile = outFillFile;
    }

    public Integer getCoutPipe() {
        return trentoP.pOutPipe;
    }

    public void setCoutPipe( Integer outPipe ) {

        trentoP.pOutPipe = outPipe;
    }

    public Double getCaccuracy() {
        return trentoP.pAccuracy;
    }

    public void setCaccuracy( Double accuracy ) {
        PreferencesHandler.setPreference(ACCURACY, accuracy.toString());
        trentoP.pAccuracy = accuracy;
    }

    public Double getCa() {
        return trentoP.pA;
    }

    public void setCa( Double a ) {
        if (a != null) {
            PreferencesHandler.setPreference(CalibrationModel.A, a.toString());

        }
        trentoP.pA = a;
    }

    public Double getCn() {
        return trentoP.pN;
    }

    public void setCn( Double n ) {
        if (n != null) {
            PreferencesHandler.setPreference(CalibrationModel.N, n.toString());
        }
        trentoP.pN = n;
    }

    public Double getCminG() {
        return trentoP.pMinG;
    }

    public void setCminG( Double minG ) {
        PreferencesHandler.setPreference(CalibrationModel.MIN_FILL_DEGREE, minG.toString());
        trentoP.pMinG = minG;
    }

    public Double getCcelerity() {
        return trentoP.pCelerityFactor;
    }

    public void setCcelerity( Double celerity ) {
        PreferencesHandler.setPreference(CalibrationModel.CELERITY, celerity.toString());
        trentoP.pCelerityFactor = celerity;

    }

    public Double getCeps() {
        return trentoP.pEpsilon;
    }

    public void setCeps( Double eps ) {
        PreferencesHandler.setPreference(CalibrationModel.EPS, eps.toString());
        trentoP.pEpsilon = eps;

    }

    public Double getCexponent() {
        return trentoP.pExponent;
    }

    public void setCexponent( Double exponent ) {
        PreferencesHandler.setPreference(CalibrationModel.EXPONENT, exponent.toString());
        trentoP.pExponent = exponent;
    }

    public Double getCgamma() {
        return trentoP.pGamma;
    }

    public void setCgamma( Double gamma ) {
        PreferencesHandler.setPreference(CalibrationModel.GAMMA, gamma.toString());
        trentoP.pGamma = gamma;

    }

    public Double getCinflux() {
        return trentoP.pEspInflux;
    }

    public void setCinflux( Double influx ) {
        PreferencesHandler.setPreference(CalibrationModel.INFLUX, influx.toString());
        trentoP.pEspInflux = influx;
    }

    public Integer getCjMax() {
        return trentoP.pJMax;
    }

    public void setCjMax( Integer jMax ) {
        PreferencesHandler.setPreference(CalibrationModel.J_MAX, jMax.toString());
        trentoP.pJMax = jMax;
    }

    public Double getCmaxFill() {
        return trentoP.pMaxTheta;
    }

    public void setCmaxFill( Double maxFill ) {
        PreferencesHandler.setPreference(CalibrationModel.MAX_FILL_DEGREE, maxFill.toString());
        trentoP.pMaxTheta = maxFill;
    }

    public Double getCtolerance() {
        return trentoP.pTolerance;
    }

    public void setCtolerance( Double tolerance ) {
        PreferencesHandler.setPreference(CalibrationModel.TOLERANCE, tolerance.toString());
        trentoP.pTolerance = tolerance;

    }

    public Integer getCmaxJunction() {
        return trentoP.pMaxJunction;
    }

    public void setCmaxJunction( Integer maxJunction ) {
        PreferencesHandler.setPreference(CalibrationModel.MAX_JUNCTION, maxJunction.toString());
        trentoP.pMaxJunction = maxJunction;
    }

    public Double getCminDischarge() {
        return trentoP.pMinDischarge;
    }

    public void setCminDischarge( Double minDischarge ) {
        PreferencesHandler.setPreference(CalibrationModel.MIN_DISCHARGE, minDischarge.toString());
        trentoP.pMinDischarge = minDischarge;

    }

    public String getCinputFile() {
        return inputRainFile;
    }

    public void setCinputFile( String inputFile ) {
        if (inputFile != null) {
            PreferencesHandler.setPreference(CalibrationModel.INPUT_RAIN_FILE, inputFile);
        }
        this.inputRainFile = inputFile;
    }

    public Integer getCdt() {

        return trentoP.dt;
    }

    public void setCdt( Integer dt ) {
        if (dt != null) {
            PreferencesHandler.setPreference(CalibrationModel.STEP, dt.toString());
        }
        trentoP.dt = dt;

    }

    public Integer getCmaxT() {

        return trentoP.tMax;
    }

    public void setCmaxT( Integer maxT ) {
        PreferencesHandler.setPreference(CalibrationModel.MAXIMUM_TIME, maxT.toString());
        trentoP.tMax = maxT;

    }

    public Integer getCtpMaxCalibration() {
        return trentoP.tpMaxCalibration;
    }

    public void setCtpMaxCalibration( Integer tpMaxCalibration ) {
        PreferencesHandler.setPreference(CalibrationModel.TP_MAX_CALIBRATION, tpMaxCalibration.toString());
        trentoP.tpMaxCalibration = tpMaxCalibration;

    }

    /**
     * Set the geosewere network.
     * 
     * @param network, a SimpleFeatureCollection which contains the geosewere network.
     */
    public void setCnetwork( SimpleFeatureCollection network ) {
        trentoP.inPipes = network;
    }

    /**
     * Get the discharge, in calibration mode.
     * 
     * @return an HashMap, for each time contains the discharge of pipes.
     */

    public HashMap<DateTime, HashMap<Integer, double[]>> getDischargeHash() {

        return trentoP.outDischarge;
    }

    /**Run the simulation.
     * 
     * @throws Exception
     */
    public void runModel() throws Exception {
        trentoP.process();
        String w = trentoP.warningBuilder.toString();
        if (!w.equals("warnings")) { //$NON-NLS-1$
            warning = w.toString();
        }
    }

    /**
     * Set the input network.
     * 
     * @param networkFC the geosewere network.
     */
    public void setInputSHP( SimpleFeatureCollection networkFC ) {
        trentoP.inPipes = networkFC;

    }
    /**
     * Set the Mode
     * @param mode 1 is calibration 0 is project.
     */
    public void setCmode( int mode ) {
        trentoP.pMode = mode;
    }
    public int getCmode() {
        return trentoP.pMode;
    }

    /**
     * Write on the disk the discharge data.
     * 
     * @param shell where to show the error message.
    
     */
    public void writeDischargeAndFillData() {
        writeOutput(outDischargeFile, "discharge", getDischargeHash()); //$NON-NLS-1$
        writeOutput(outFillFile, "fillDegree", getFillHash()); //$NON-NLS-1$

    }

    /**
     * Utility to write in a file the hashmap.
     * 
     * @param shell
     * @param tmpOutFile a string which is the path of the file.
     * @param tableName the name of the OMS3 table.
     * @param tmpHM the Map to write down.
     */
    private void writeOutput( String tmpOutFile, String tableName, HashMap<DateTime, HashMap<Integer, double[]>> tmpHM ) {
        try {

            // set the data format.
            DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;
            if (tmpOutFile != null) {
                OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
                // set the path of the file.
                StringBuilder str = new StringBuilder(tableName);
                if (trentoP.pN != null && trentoP.pA != null && trentoP.outTpMax != null) {

                    str.append(" a=");
                    str.append(trentoP.pA);
                    str.append(" n= ");
                    str.append(trentoP.pN);
                    str.append(" tp= ");
                    str.append(trentoP.outTpMax);
                    writer.inTablename = str.toString();
                } else {
                    writer.inTablename = tableName;
                }
                writer.file = tmpOutFile;
                writer.fileNovalue = "-9999.0"; //$NON-NLS-1$
                Iterator<DateTime> iterator1 = tmpHM.keySet().iterator();
                writer.tTimestep = getCdt();
                // set the first time steep.
                if (iterator1.hasNext()) {
                    DateTime time = iterator1.next();
                    writer.tStart = time.toString(formatter);
                    writer.inData = tmpHM.get(time);
                    writer.writeNextLine();
                }
                // write the other time steep
                while( iterator1.hasNext() ) {
                    DateTime time = iterator1.next();
                    writer.inData = tmpHM.get(time);
                    writer.writeNextLine();

                }
                writer.close();
            }
        } catch (IOException e) {
            GuiUtilities.showErrorMessage(null, "An error occurred while writing: " + outDischargeFile);
        }
    }
    /**
     * Set the parameters to run calibration mode.
     * 
     * <p>
     * When the wizard performedFinisch, and all the models parameters are set, then set it in the OMS3 model. 
     * </p>
     */
    public void setCalibrationPar() throws IOException {
        try {
            if (inputRainFile != null) {
                OmsTimeSeriesReader rainReader = new OmsTimeSeriesReader();
                rainReader.fileNovalue = "-9999"; //$NON-NLS-1$
                rainReader.file = inputRainFile;;
                rainReader.read();
                rainReader.close();
                trentoP.inRain = rainReader.outData;
            }
        } catch (IOException e) {
            GuiUtilities.showErrorMessage(null, "An error occurred while reading: " + inputRainFile);
            throw new IOException();
        }
    }

    public HashMap<DateTime, HashMap<Integer, double[]>> getFillHash() {
        return trentoP.outFillDegree;
    }

}
