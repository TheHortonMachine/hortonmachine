package org.hortonmachine.trentop.models;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureWriter;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.OmsTrentoP;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.DiametersReader;
import org.hortonmachine.modules.TrentoP;
import org.joda.time.DateTime;
import org.opengis.referencing.FactoryException;

/**
 * A dataBinding class to run TrentoP in project mode. 
 * 
 * <p> this class is used as a wrapper to TrentoP OMS models. The aim of this class is to update the models parameters with the  jface wizard through DataBinding. </p>
 * <p> For more details about the models parameter see  {@link TrentoP} .
 *
 * @author  Daniele Andreis.
 * @see   {@link TrentoP}  
 */
public class ProjectModel {
    /*
     * a string which represent the network shp file.
     */
    private static final String NETWORK_DIAMETERS_SHP = "networkDiameters.shp"; //$NON-NLS-1$

    /**
     * a string which represent the network shp file.
     */
    public static final String NETWORK_DIAMETERS = "networkDiameters"; //$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for accuracy.
     */
    public static final String ACCURACY = "paccuracy"; //$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for a.
     */
    public static final String A = "pa";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for n.
     */
    public static final String N = "pn";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for align.
     */
    public static final String ALIGN = "palign";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for tau.
     */
    public static final String TAU = "ptau";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for g.
     */
    public static final String G = "pg";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for celerity.
     */
    public static final String CELERITY = "pcelerity";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for esp.
     */
    public static final String EPS = "peps";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for exponent.
     */
    public static final String EXPONENT = "pexponent";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for gamma.
     */
    public static final String GAMMA = "pgamma";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for influx.
     */
    public static final String INFLUX = "pinflux";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for jMax.
     */
    public static final String J_MAX = "pjMax";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for maxFillDegree.
     */
    public static final String MAX_FILL_DEGREE = "pmaxFill";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for tollerance.
     */
    public static final String TOLERANCE = "ptolerance";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for c.
     */
    public static final String C = "pc";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for maxJunction.
     */
    public static final String MAX_JUNCTION = "pmaxJunction";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for minDepth.
     */
    public static final String MIN_DEPTH = "pminDepth";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for minDischarge.
     */
    public static final String MIN_DISCHARGE = "pminDischarge";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for minFillDegree.
     */
    public static final String MIN_FILL_DEGREE = "pminG";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for step time in project.
     */
    public static final String PROJECT_STEP = "ppDt";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for maximum simulation time in project.
     */
    public static final String PROJECT_MAXIMUM_TIME = "ppMaxT";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for minimum simulation time in project.
     */
    public static final String PROJECT_MINIMUM_TIME = "ppMinT";//$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#value} for the outlet of the geosewere network.
     */
    public static final String OUT_PIPE = "poutPipe"; //$NON-NLS-1$
    /**
     * A key to use get and set method in {@link PojoProperties#inputFile} which can be a diameters or rainData file..
     */
    public static final String INPUT_DIAMETER_FILE = "pinputFile"; //$NON-NLS-1$
    /*
     * The models parameters.
     */
    private OmsTrentoP trentoP;
    /*
     * Where to extract the rain data or diameters.
     */
    private String inputFile;
    /**
     * The warning that come out from the models.
     */
    private String warning = null;
    public ProjectModel() {
        trentoP = new OmsTrentoP();
    }
    public String getWarning() {
        return warning;
    }

    public Integer getPoutPipe() {
        return trentoP.pOutPipe;
    }

    public void setPoutPipe( Integer outPipe ) {

        trentoP.pOutPipe = outPipe;
    }

    public Double getPaccuracy() {
        return trentoP.pAccuracy;
    }

    public void setPaccuracy( Double accuracy ) {
        PreferencesHandler.setPreference(ProjectModel.ACCURACY, accuracy.toString());
        trentoP.pAccuracy = accuracy;
    }

    public Double getPa() {
        return trentoP.pA;
    }

    public void setPa( Double a ) {
        if (a != null) {
            PreferencesHandler.setPreference(ProjectModel.A, a.toString());
        }
        trentoP.pA = a;
    }

    public Double getPn() {
        return trentoP.pN;
    }

    public void setPn( Double n ) {
        if (n != null) {
            PreferencesHandler.setPreference(ProjectModel.N, n.toString());
        }
        trentoP.pN = n;
    }

    public Double getPminG() {
        return trentoP.pMinG;
    }

    public void setpminG( Double minG ) {
        PreferencesHandler.setPreference(ProjectModel.MIN_FILL_DEGREE, minG.toString());
        trentoP.pMinG = minG;
    }

    public Integer getPalign() {
        return trentoP.pAlign;
    }

    public void setPalign( Integer align ) {
        PreferencesHandler.setPreference(ProjectModel.ALIGN, align.toString());
        trentoP.pAlign = align;
    }

    public Double getPtau() {
        return trentoP.pTau;
    }

    public void setPtau( Double tau ) {
        PreferencesHandler.setPreference(ProjectModel.TAU, tau.toString());
        trentoP.pTau = tau;
    }

    public Double getPg() {
        return trentoP.pG;
    }

    public void setPg( Double g ) {
        PreferencesHandler.setPreference(ProjectModel.G, g.toString());
        trentoP.pG = g;

    }

    public Double getPcelerity() {
        return trentoP.pCelerityFactor;
    }

    public void setPcelerity( Double celerity ) {
        PreferencesHandler.setPreference(ProjectModel.CELERITY, celerity.toString());
        trentoP.pCelerityFactor = celerity;

    }

    public Double getPeps() {
        return trentoP.pEpsilon;
    }

    public void setPeps( Double eps ) {
        PreferencesHandler.setPreference(ProjectModel.EPS, eps.toString());
        trentoP.pEpsilon = eps;

    }

    public Double getPexponent() {
        return trentoP.pExponent;
    }

    public void setPexponent( Double exponent ) {
        PreferencesHandler.setPreference(ProjectModel.EXPONENT, exponent.toString());
        trentoP.pExponent = exponent;
    }

    public Double getPgamma() {
        return trentoP.pGamma;
    }

    public void setPgamma( Double gamma ) {
        PreferencesHandler.setPreference(ProjectModel.GAMMA, gamma.toString());
        trentoP.pGamma = gamma;

    }

    public Double getPinflux() {
        return trentoP.pEspInflux;
    }

    public void setPinflux( Double influx ) {
        PreferencesHandler.setPreference(ProjectModel.INFLUX, influx.toString());
        trentoP.pEspInflux = influx;
    }

    public Integer getPjMax() {
        return trentoP.pJMax;
    }

    public void setPjMax( Integer jMax ) {
        PreferencesHandler.setPreference(ProjectModel.J_MAX, jMax.toString());
        trentoP.pJMax = jMax;
    }

    public Double getPmaxFill() {
        return trentoP.pMaxTheta;
    }

    public void setPmaxFill( Double maxFill ) {
        PreferencesHandler.setPreference(ProjectModel.MAX_FILL_DEGREE, maxFill.toString());
        trentoP.pMaxTheta = maxFill;
    }

    public Double getPtolerance() {
        return trentoP.pTolerance;
    }

    public void setPtolerance( Double tolerance ) {
        PreferencesHandler.setPreference(ProjectModel.TOLERANCE, tolerance.toString());
        trentoP.pTolerance = tolerance;

    }

    public Integer getPmaxJunction() {
        return trentoP.pMaxJunction;
    }

    public void setPmaxJunction( Integer maxJunction ) {
        PreferencesHandler.setPreference(ProjectModel.MAX_JUNCTION, maxJunction.toString());
        trentoP.pMaxJunction = maxJunction;
    }

    public Double getPminDepth() {
        return trentoP.pMinimumDepth;
    }

    public void setPminDepth( Double minDepth ) {
        PreferencesHandler.setPreference(ProjectModel.MIN_DEPTH, minDepth.toString());
        trentoP.pMinimumDepth = minDepth;

    }

    public Double getPminDischarge() {
        return trentoP.pMinDischarge;
    }

    public void setPminDischarge( Double minDischarge ) {
        PreferencesHandler.setPreference(ProjectModel.MIN_DISCHARGE, minDischarge.toString());
        trentoP.pMinDischarge = minDischarge;

    }

    public Double getPc() {
        return trentoP.pC;
    }

    public void setPc( Double c ) {
        PreferencesHandler.setPreference(ProjectModel.C, c.toString());
        trentoP.pC = c;

    }

    public String getPinputFile() {
        return inputFile;
    }

    public void setPinputFile( String inputFile ) {
        if (inputFile != null) {
            PreferencesHandler.setPreference(ProjectModel.INPUT_DIAMETER_FILE, inputFile);
        }
        this.inputFile = inputFile;
    }

    public Double getPpDt() {
        return trentoP.tDTp;

    }

    public void setPpDt( Double pDt ) {
        if (pDt != null) {
            PreferencesHandler.setPreference(ProjectModel.PROJECT_STEP, pDt.toString());
            trentoP.tDTp = pDt;
        }

    }
    public Double getPpMaxT() {
        return trentoP.tpMax;
    }

    public void setPpMaxT( Double pMaxT ) {
        PreferencesHandler.setPreference(ProjectModel.PROJECT_MAXIMUM_TIME, pMaxT.toString());
        trentoP.tpMax = pMaxT;

    }

    public Double getPpMinT() {
        return trentoP.tpMin;
    }

    public void setPpMinT( Double pMinT ) {
        PreferencesHandler.setPreference(ProjectModel.PROJECT_MINIMUM_TIME, pMinT.toString());
        trentoP.tpMin = pMinT;
    }

    /**
     * Set the geosewere network.
     * 
     * @param network, a SimpleFeatureCollection which contains the geosewere network.
     */
    public void setPnetwork( SimpleFeatureCollection network ) {
        trentoP.inPipes = network;
    }

    /**
     * Get the project pipes.
     * 
     * @return the FeatureCollection with the diameters of the pipes.
     * @throws Exception
     */
    public SimpleFeatureCollection getPoutNetwork() throws Exception {

        return trentoP.outPipes;
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
    public void setPmode( int mode ) {
        trentoP.pMode = mode;
    }
    public int getPmode() {
        return trentoP.pMode;
    }
    /**
     * Write in the disk the output FeatureCollection.
     * 
     * @param networkPath how to write it.  
     * @param shell where to show the error message.
     * @throws FactoryException
     * @throws MalformedURLException 
     */
    public void writeNetworkShp( String networkPath ) {
        try {
            OmsShapefileFeatureWriter.writeShapefile(new File(networkPath, NETWORK_DIAMETERS_SHP).getAbsolutePath(),
                    trentoP.outPipes, null);
//            Utility.addServiceToCatalogAndMap(networkPath + File.separator + NETWORK_DIAMETERS_SHP, true, true, null);
        } catch (IOException e) {
            GuiUtilities.showErrorMessage(null, "An error occurred while writing: " + networkPath);
        }

    }

    /**
     * Set the parameters to run project mode.
     * 
     * <p>
     * When the wizard performedFinisch, and all the models parameters are set, then set it in the OMS3 model. this set the commercial diameters value. 
     * </p>
     * @param shell where to show the error message.
     */
    public void setProjectPar() {
        // read the diameters.
        try {
            DiametersReader diametersreader = new DiametersReader();
            diametersreader.file = this.inputFile;
            diametersreader.pCols = 2;
            diametersreader.pSeparator = "\\s+"; //$NON-NLS-1$
            diametersreader.fileNovalue = "-9999.0"; //$NON-NLS-1$
            diametersreader.readFile();
            trentoP.inDiameters = diametersreader.data;
        } catch (IOException e) {
            GuiUtilities.showErrorMessage(null, "An error occurred while reading: " + inputFile);
        }
    }

    public HashMap<DateTime, HashMap<Integer, double[]>> getFillHash() {
        return trentoP.outFillDegree;
    }
}
