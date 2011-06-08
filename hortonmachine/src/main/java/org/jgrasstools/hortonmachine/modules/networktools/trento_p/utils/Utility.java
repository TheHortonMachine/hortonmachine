/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.math.functions.MinimumFillDegreeFunction;
import org.jgrasstools.gears.utils.math.rootfinding.RootFindingFunctions;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.net.Pipe;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

/**
 * It's a collection of method used only for the TrentoP project.
 * 
 * @author Daniele Andreis, Riccardo Rigon, David tamanini.
 * 
 */

public class Utility {

    static final String USER_DIR_KEY = "user.dir";

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
     * @param sup upper extremm value of the research.
     * @param known is a constant, evaluated in the main of the program.
     * @param twooverthree2, constant.
     * @param minG fill degree.
     * @param accuracy.
     * @param jMax maximum number of iteration.
     * @param pm
     * @return the fill degree
     * 
     */

    public static double thisBisection( double sup, double known, double twooverthree2, double minG, double accuracy,
            double jMax, IJGTProgressMonitor pm, StringBuilder strWarnings ) {

        double thetai = 0;
        /* Ampiezza dell'intervallo per il bracketing */
        double delta;
        /*
         * Estremo inferiore da considerare nella ricerca del angolo di
         * riempimento che si ottiene adottando il diametro commerciale, anziche
         * quello calcolato
         */
        double lower = 0;
        /*
         * Estremo superiore da adottare nella ricerca dell'angolo formato dalla
         * sezione bagnata, relativa al diametro commerciale
         */
        double higher;
        /* ampiezza dell'intervallo per effettuare il bracketing */
        delta = sup / 10;
        /*
         * e una funzione che mi calcola l'estremo superiore da usare nella
         * ricerca del'angolo di riempimento con metodo di bisezione
         */
        MinimumFillDegreeFunction gsmFunction = new MinimumFillDegreeFunction();
        gsmFunction.setParameters(known, twooverthree2, minG);
        higher = gsmFunction.getValue(sup);

        int i;
        /*
         * questo ciclo for mi consente di decrementare l'angolo theta di delta,
         * me fermo solo quando trovo l'estremo inferiore per un bracketing
         * corretto
         */
        for( i = 0; i < 10; i++ ) {
            thetai = sup - (i + 1) * delta;

            lower = gsmFunction.getValue(thetai);

            /* Ho trovato due punti in cui la funzione assume segno opposto */{
                if (higher * lower < 0)
                    break;
            }
        }
        /*
         * Il bracketing non ha avuto successo, restituisco il riempimento
         * minimo.
         */
        if (i == 11 && lower * higher > 0) {
            strWarnings.append(msg.message("trentoP.warning.minimumDepth"));
            return minG;
        }
        /*
         * chiamata alla funzione rtbis(), che restiuira la radice cercata con
         * la precisione richiesta.
         */
        gsmFunction.setParameters(known, twooverthree2, minG);
        return RootFindingFunctions.rtbis(gsmFunction, thetai, thetai + delta, accuracy, jMax, pm);
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
        SimpleFeatureCollection outPipesFC = FeatureCollections.newCollection();
        SimpleFeatureBuilder builderFeature = new SimpleFeatureBuilder(type);
        FeatureIterator<SimpleFeature> stationsIter = inPipesFC.features();
        try {
            int t;
            while( stationsIter.hasNext() ) {
                SimpleFeature feature = stationsIter.next();
                try {
                    t = ((Number) feature.getAttribute(TrentoPFeatureType.ID_STR)).intValue();
                    t = t - 1;
                    Geometry line = (Geometry) feature.getDefaultGeometry();
                    builderFeature.add(line);
                    builderFeature.add(networkPipes[t].getId());
                    builderFeature.add(networkPipes[t].getIdPipeWhereDrain());
                    builderFeature.add(networkPipes[t].getIdPipeWhereDrain());
                    builderFeature.add(networkPipes[t].getInitialElevation());
                    builderFeature.add(networkPipes[t].getFinalElevation());
                    builderFeature.add(networkPipes[t].getRunoffCoefficient());
                    builderFeature.add(networkPipes[t].getAverageResidenceTime());
                    builderFeature.add(networkPipes[t].getKs());
                    builderFeature.add(networkPipes[t].getMinimumPipeSlope());
                    builderFeature.add(networkPipes[t].getPipeSectionType());
                    builderFeature.add(networkPipes[t].getAverageSlope());
                    builderFeature.add(networkPipes[t].diameterToVerify);
                    builderFeature.add(networkPipes[t].verifyPipeSlope);
                    builderFeature.add(networkPipes[t].discharge);
                    builderFeature.add(networkPipes[t].coeffUdometrico);
                    builderFeature.add(networkPipes[t].residenceTime);
                    builderFeature.add(networkPipes[t].tP);
                    builderFeature.add(networkPipes[t].residenceTime);
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

                } catch (NullPointerException e) {
                    throw new IllegalArgumentException(msg.message("trentop.illegalNet" + "in output"));

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
     * @param two array which contains where a pipe drains.
     * @param pm the progerss monitor.
     * @throw IllegalArgumentException if the water through a number of state
     *        which is greater than the state in the network.
     */
    public static void pipeMagnitude( double[] magnitude, double[] two, IJGTProgressMonitor pm ) {

        int count = 0;

        /* two Contiene gli indici degli stati riceventi. */
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
            while( two[k] != 0 && count < length ) {
                k = (int) two[k] - 1;
                magnitude[k]++;
                count++;
            }
            if (count > length) {
                pm.errorMessage(msg.message("trentoP.error.pipe"));
                throw new IllegalArgumentException(msg.message("trentoP.error.pipe"));
            }
        }

    }

   public static void makePolygonShp( ITrentoPType[] values, File baseFolder, CoordinateReferenceSystem crs, String pAreaShapeFileName ) throws IOException {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        String typeName= values[0].getName();
        b.setName(typeName);
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add(PipesTrentoP.ID.getAttributeName(),PipesTrentoP.ID.getClazz() );
        SimpleFeatureType tanksType = b.buildFeatureType();
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        File file = new File(baseFolder, pAreaShapeFileName);
        Map<String, Serializable> create = new HashMap<String, Serializable>();
        create.put("url", file.toURI().toURL());
        ShapefileDataStore newDataStore = (ShapefileDataStore) factory.createNewDataStore(create);
        newDataStore.createSchema(tanksType);
        Transaction transaction = new DefaultTransaction();
        FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore
                .getFeatureSource();
        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(FeatureCollections.newCollection());
            transaction.commit();
        } catch (Exception problem) {
            problem.printStackTrace();
            transaction.rollback();
        } finally {
            transaction.close();
        }
    }

    /**
     * Build the shapefile.
     * 
     * @param types the geometry type.
     * @param baseFolder the folder where to put the file.
     * @param mapCrs the name of the crs.
     * @param pShapeFileName 
     * @param networkFC 
     * @throws MalformedURLException
     * @throws IOException
     */
    public static void makeLineStringShp( ITrentoPType[] types, File baseFolder, CoordinateReferenceSystem mapCrs, String pShapeFileName, SimpleFeatureCollection networkFC )
            throws MalformedURLException, IOException {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        String typeName = types[0].getName();
        b.setName(typeName);
        b.setCRS(mapCrs);
        b.add("the_geom", LineString.class);
        for( ITrentoPType type : types ) {
            b.add(type.getAttributeName(), type.getClazz());
        }
        SimpleFeatureType tanksType = b.buildFeatureType();
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        File file = new File(baseFolder, pShapeFileName);
        Map<String, Serializable> create = new HashMap<String, Serializable>();
        create.put("url", file.toURI().toURL());
        ShapefileDataStore newDataStore = (ShapefileDataStore) factory.createNewDataStore(create);
        newDataStore.createSchema(tanksType);
        Transaction transaction = new DefaultTransaction();
        FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore
                .getFeatureSource();
        featureStore.setTransaction(transaction);
        try {
            if(networkFC==null){
            featureStore.addFeatures(FeatureCollections.newCollection());
            }else{
                featureStore.addFeatures(networkFC);

            }
            transaction.commit();
        } catch (Exception problem) {
            problem.printStackTrace();
            transaction.rollback();
        } finally {
            transaction.close();
        }
    }
}
