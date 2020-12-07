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
package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;
import static org.hortonmachine.gears.utils.features.FeatureUtilities.findAttributeName;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.hortonmachine.gears.utils.math.functions.MinimumFillDegreeFunction;
import org.hortonmachine.gears.utils.math.rootfinding.RootFindingFunctions;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.hortonmachine.hmachine.modules.networktools.trento_p.net.Pipe;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * It's a collection of method used only for the OmsTrentoP project.
 * 
 * @author Daniele Andreis, Riccardo Rigon, David tamanini.
 * 
 */

public class Utility {

    public static NumberFormat F = new DecimalFormat("#.##############");
    public static NumberFormat F_INT = new DecimalFormat("#");

    private static HortonMessageHandler msg = HortonMessageHandler.getInstance();

    /**
     * Bracketing.
     * 
     * <p>
     * It does the bracketing and then search the root of some function (fill
     * degree, if it's used a commercial diameters):
     * <ol>
     * <li>At first, does the bracketig, in order to search the roots with the
     * bisection method.
     * <li>Call the rtbis function, to evalutate the GSM function root.
     * <li>Return the fill degree, from rtbis.
     * </ol>
     * </p>
     * 
     * @param sup upper extrem value of the research.
     * @param known is a constant, evaluated in the main of the program.
     * @param twooverthree, constant.
     * @param minG fill degree.
     * @param accuracy.
     * @param jMax maximum number of iteration.
     * @param pm progress monitor
     * @param strWarnings a StringBuilder where to put the warning messages.
     * @return the fill degree
     * 
     */

    public static double thisBisection( double sup, double known, double twooverthree, double minG, double accuracy, double jMax,
            IHMProgressMonitor pm, StringBuilder strWarnings ) {

        double thetai = 0;
        /* Ampiezza dell'intervallo per il bracketing */
        double delta;
        /*
         * Estremo inferiore da considerare nella ricerca del angolo di
         * riempimento che si ottiene adottando il diametro commerciale, anziche
         * quello calcolato
         */
        double lowerLimit = 0;
        /*
         * Estremo superiore da adottare nella ricerca dell'angolo formato dalla
         * sezione bagnata, relativa al diametro commerciale
         */
        double upperLimit;
        /* ampiezza dell'intervallo per effettuare il bracketing */
        delta = sup / 10;
        /*
         * e una funzione che mi calcola l'estremo superiore da usare nella
         * ricerca del'angolo di riempimento con metodo di bisezione
         */
        MinimumFillDegreeFunction gsmFunction = new MinimumFillDegreeFunction();
        gsmFunction.setParameters(known, twooverthree, minG);
        upperLimit = gsmFunction.getValue(sup);

        int i;
        /*
         * questo ciclo for mi consente di decrementare l'angolo theta di delta,
         * mi fermo solo quando trovo l'estremo inferiore per un bracketing
         * corretto
         */
        for( i = 0; i < 10; i++ ) {
            thetai = sup - (i + 1) * delta;

            lowerLimit = gsmFunction.getValue(thetai);

            /* Ho trovato due punti in cui la funzione assume segno opposto */ {
                if (upperLimit * lowerLimit < 0)
                    break;
            }
        }
        /*
         * Il bracketing non ha avuto successo, restituisco il riempimento
         * minimo.
         */
        if (i == 11 && lowerLimit * upperLimit > 0) {
            strWarnings.append(msg.message("trentoP.warning.minimumDepth"));
            return minG;
        }
        /*
         * chiamata alla funzione rtbis(), che restiuira la radice cercata con
         * la precisione richiesta.
         */
        gsmFunction.setParameters(known, twooverthree, minG);
        if (Math.abs(thetai) <= NumericsUtilities.machineFEpsilon()) {
            thetai = 0.0;
        }
        return RootFindingFunctions.bisectionRootFinding(gsmFunction, thetai, thetai + delta, accuracy, jMax, pm);
    }

    /**
     * 
     * Create a FutureCollection from the input data and the output data.
     * 
     * @result outPipesFC where store the data.
     * @param inPipesFC where are stored the network input data.
     * @param networkPipes where are stored the output value.
     * @throws IOException 
     */
    public static SimpleFeatureCollection createFeatureCollections( SimpleFeatureCollection inPipesFC, Pipe[] networkPipes )
            throws IOException {
        if (inPipesFC == null) {
            throw new RuntimeException();
        }

        /*
         * Create a Type
         */
        ITrentoPType[] values = TrentoPFeatureType.PipesTrentoP.values();
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        String typeName = values[0].getName();
        builder.setName(typeName);
        builder.setCRS(inPipesFC.features().next().getType().getCoordinateReferenceSystem());
        builder.add("the_geom", LineString.class);
        for( ITrentoPType type : values ) {
            builder.add(type.getAttributeName(), type.getClazz());
        }

        SimpleFeatureType type = builder.buildFeatureType();

        /*
         * Create The Feature Collection. Extract the geometry data from the
         * inPipesFC and other data from networkPipe.
         */
        DefaultFeatureCollection outPipesFC = new DefaultFeatureCollection();
        SimpleFeatureBuilder builderFeature = new SimpleFeatureBuilder(type);
        FeatureIterator<SimpleFeature> stationsIter = inPipesFC.features();
        String searchedField = PipesTrentoP.PER_AREA.getAttributeName();
        String attributeName = findAttributeName(inPipesFC.getSchema(), searchedField);
        try {
            int t = 0;
            while( stationsIter.hasNext() ) {
                SimpleFeature feature = stationsIter.next();
                try {
                    Geometry line = (Geometry) feature.getDefaultGeometry();
                    builderFeature.add(line);
                    builderFeature.add(networkPipes[t].getId());
                    builderFeature.add(networkPipes[t].getIdPipeWhereDrain());
//                    builderFeature.add(networkPipes[t].getDrainArea()); //getAttribute(TrentoPFeatureType.DRAIN_AREA_STR)).doubleValue());
//                    builderFeature.add(networkPipes[t].getInitialElevation());
//                    builderFeature.add(networkPipes[t].getFinalElevation());
                    builderFeature.add(networkPipes[t].getRunoffCoefficient());
                    builderFeature.add(networkPipes[t].getAverageResidenceTime());
                    builderFeature.add(networkPipes[t].getKs());
                    builderFeature.add(networkPipes[t].getMinimumPipeSlope());
                    builderFeature.add(networkPipes[t].getPipeSectionType());
                    builderFeature.add(networkPipes[t].getAverageSlope());
                    if (attributeName != null) {
                        builderFeature
                                .add(((Number) feature.getAttribute(TrentoPFeatureType.PERCENTAGE_OF_DRY_AREA)).doubleValue());
                    } else {
                        builderFeature.add(1.0);
                    }
                    builderFeature.add(networkPipes[t].discharge);
                    builderFeature.add(networkPipes[t].coeffUdometrico);
                    builderFeature.add(networkPipes[t].residenceTime);
                    builderFeature.add(networkPipes[t].tP);
                    builderFeature.add(networkPipes[t].tQmax);
                    builderFeature.add(networkPipes[t].meanSpeed);
                    builderFeature.add(networkPipes[t].pipeSlope);
                    builderFeature.add(networkPipes[t].diameter);
                    builderFeature.add(networkPipes[t].emptyDegree);
                    builderFeature.add(networkPipes[t].depthInitialPipe);
                    builderFeature.add(networkPipes[t].depthFinalPipe);
                    builderFeature.add(networkPipes[t].initialFreesurface);
                    builderFeature.add(networkPipes[t].finalFreesurface);
                    builderFeature.add(networkPipes[t].totalSubNetArea);
                    builderFeature.add(networkPipes[t].totalSubNetLength);
                    builderFeature.add(networkPipes[t].meanLengthSubNet);
                    builderFeature.add(networkPipes[t].varianceLengthSubNet);

                    SimpleFeature featureOut = builderFeature.buildFeature(null);
                    outPipesFC.add(featureOut);
                    t++;
                } catch (NullPointerException e) {
                	e.printStackTrace();
                	throw new IllegalArgumentException(msg.message("Illegal content of the input network."));

                }
            }

        } finally {
            stationsIter.close();
        }
        return outPipesFC;
    }

    /**
     * Calculate the magnitudo of the several drainage area.
     * 
     * <p>
     * It begin from the first state, where the magnitudo is setted to 1, and
     * then it follow the path of the water until the outlet.
     * </p>
     * <p>
     * During this process the magnitudo of each through areas is incremented of
     * 1 units. This operation is done for every state, so any path of the water
     * are analized, from the state where she is intercept until the outlet.
     * <p>
     * </p>
     * 
     * @param magnitudo is the array where the magnitudo is stored.
     * @param whereDrain array which contains where a pipe drains.
     * @param pm the progerss monitor.
     * @throw IllegalArgumentException if the water through a number of state
     *        which is greater than the state in the network.
     */
    public static void pipeMagnitude( double[] magnitude, double[] whereDrain, IHMProgressMonitor pm ) {

        int count = 0;

        /* whereDrain Contiene gli indici degli stati riceventi. */
        /* magnitude Contiene la magnitude dei vari stati. */

        int length = magnitude.length;
        /* Per ogni stato */
        for( int i = 0; i < length; i++ ) {
            count = 0;
            /* la magnitude viene posta pari a 1 */
            magnitude[i]++;
            int k = i;
            /*
             * Si segue il percorso dell'acqua e si incrementa di 1 la mgnitude
             * di tutti gli stati attraversati prima di raggiungere l'uscita
             */
            while( whereDrain[k] != Constants.OUT_INDEX_PIPE && count < length ) {
                k = (int) whereDrain[k];
                magnitude[k]++;
                count++;
            }
            if (count > length) {
                pm.errorMessage(msg.message("trentoP.error.pipe"));
                throw new IllegalArgumentException(msg.message("trentoP.error.pipe"));
            }
        }

    }

    /**
     * Verify the schema.
     * 
     * <p>
     * Verify if the FeatureCollection contains all the needed fields.
     * </p>
     * @param schema is yhe type for the calibration mode.
     * @param pm
     * @return true if there is the percentage of the area.
     */
    public static boolean verifyCalibrationType( SimpleFeatureType schema, IHMProgressMonitor pm ) {
        String searchedField = PipesTrentoP.ID.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.RUNOFF_COEFFICIENT.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.KS.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.DIAMETER.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.AVERAGE_SLOPE.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.AVERAGE_RESIDENCE_TIME.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.PER_AREA.getAttributeName();
        String attributeName = findAttributeName(schema, searchedField);
        if (attributeName != null) {
            return true;
        }

        return false;
    }

    /**
     * Verify the schema.
     * 
     * <p>
     * Verify if the FeatureCollection contains all the needed fields.
     * </p>
     * @param schema is yhe type for the project mode.
     * @param pm
     * @return true if there is the percentage of the area.
     */
    public static boolean verifyProjectType( SimpleFeatureType schema, IHMProgressMonitor pm ) {
        String searchedField = PipesTrentoP.ID.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.RUNOFF_COEFFICIENT.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.KS.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.MINIMUM_PIPE_SLOPE.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.AVERAGE_RESIDENCE_TIME.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.PIPE_SECTION_TYPE.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.AVERAGE_SLOPE.getAttributeName();
        verifyFeatureKey(findAttributeName(schema, searchedField), searchedField, pm);
        searchedField = PipesTrentoP.PER_AREA.getAttributeName();
        String attributeName = findAttributeName(schema, searchedField);
        if (attributeName != null) {
            return true;
        }

        return false;

    }

    /**
     * Verify if there is a key of a FeatureCollections.
     * 
     * @param key
     * @throws IllegalArgumentException
     *             if the key is null.
     */
    private static void verifyFeatureKey( String key, String searchedField, IHMProgressMonitor pm ) {
        if (key == null) {
            if (pm != null) {
                pm.errorMessage(msg.message("trentoP.error.featureKey") + searchedField);
            }
            throw new IllegalArgumentException(msg.message("trentoP.error.featureKey") + searchedField);
        }

    }

    /**
     * Calculate the fill degree of a pipe.
     * 
     * @param theta the angle between the free surface and the center of the pipe.
     * @return the value of y/D.
     */
    public static double angleToFillDegree( double theta ) {
        return 0.5 * (1 - Math.cos(theta / 2));

    }

}
