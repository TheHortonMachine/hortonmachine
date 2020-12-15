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
package org.hortonmachine.hmachine.modules.networktools.trento_p.net;

import static java.lang.Math.acos;
import static java.lang.Math.ceil;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.CUBICMETER2LITER;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_C;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_J_MAX;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MAX_JUNCTION;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MAX_THETA;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MING;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MINIMUM_DEPTH;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_MIN_DISCHARGE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TOLERANCE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.EIGHTOVERTHREE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.FIVEOVEREIGHT;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.HOUR2MIN;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.METER2CM;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.METER2CMSQUARED;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.ONEOVERFOUR;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.ONEOVERSIX;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.ONEOVERTHIRTEEN;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.SEVENOVERTHIRTEEN;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.SIXOVERTHIRTEEN;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.THIRTHEENOVERSIX;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.THREEOVEREIGHT;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.TWOOVERTHREE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.TWO_TENOVERTHREE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.TWO_THIRTEENOVEREIGHT;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.TWO_THIRTEENOVERTHREE;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.TWO_TWENTYOVERTHIRTEEN;
import static org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Constants.WSPECIFICWEIGHT;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.Utility;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.PipeCombo;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * This is a class which represents a sewer pipe.
 * 
 * <p>
 * It contains a collection of field which are a pipe features. Some of these
 * are private, and are used as input to calculate the others.
 * 
 * There is two type of features: geometric and hydraulic.
 * 
 * The depth are refereed to the ground and the elevation to the see level.
 * </p>
 * <p>
 * In this class there is some methods that are used to calculate feature that
 * can be calculate only with few external parameters. The other properties
 * need, to be calculate, to the whole network so they are put into other
 * classes ({@link NetworkBuilder, @}{@link OmsTrentoP}).
 * 
 * </p>
 * 
 * <p>
 * Some of the properties of a pipe are private and theirs fields are set at the
 * initializing time and the value is immutable (the initial value have to be
 * the same as the final) so there is only the getter method. Others fields are
 * set as a public and in the implementations of OmsTrentoP program are set
 * directly. This is done to obtain a more readable code, in fact in the code
 * there is some large equations that aren't useful to read in a setter method.
 * </p>
 * 
 * 
 * @author Daniele Andreis, Riccardo Rigon, David Tamanini.
 * @see {@link OmsTrentoP}
 * @version since 1.1
 * 
 */

public class Pipe {

    /**
     * Accuracy used with the bisection method.
     */
    private double accuracy;
    /**
     * Minimum dig depth for the circular pipe.
     */
    private double minimumDepth = DEFAULT_MINIMUM_DEPTH;
    /**
     * Max number of bisection to do in the solution of equationon.
     */
    private int jMax = DEFAULT_J_MAX;
    /**
     * Minimum fill degree in the channel.
     */
    private double minG = DEFAULT_MING;
    /**
     * Maximum Fill degree.
     */
    private double maxTheta = DEFAULT_MAX_THETA;
    /**
     * Align mode, it can be 0 (so the free surface is aligned through a change
     * in the depth of the pipes) or 1 (aligned with bottom step).
     */
    private int align;
    /**
     * Max number of pipes that can converge in a junction.
     */
    private int maxJunction = DEFAULT_MAX_JUNCTION;
    /**
     * Minimum discharge in a pipe.
     * 
     */
    private double minDischarge = DEFAULT_MIN_DISCHARGE;
    /**
     * Tangential bottom stress, which ensure the self-cleaning of the network.
     */
    private double tau;
    /**
     * Fill degree to use during the project.
     */
    private double g;
    /**
     * Division base to height in the rectangular or trapezium section.
     * 
     */
    private double c = DEFAULT_C;
    /**
     * Tolerance used in the iteration method to calculate the radius.
     */
    private double tolerance = DEFAULT_TOLERANCE;
    /**
     * It's the ID of this pipe.
     */
    private int id;
    /**
     * It's the ID of the pipe where this drain.
     */
    private Integer idPipeWhereDrain = null;
    /**    
     * It's the ID of the pipe where this drain.
     */
    private Integer indexPipeWhereDrain = null;
    /**
     * Is the contributed area.
     */
    private double drainArea;
    /**
     * The pipe's length.
     */
    private double lenght;
    /**
     * The elevation of the begin of the pipe.
     */
    private double initialElevation;
    /**
     * The elevation of the end of the pipe.
     */
    private double finalElevation;
    /**
     * The run of coefficient related to the area which drains in this pipe.
     */
    private double runoffCoefficient;
    /**
     * The average residence time.
     */
    private double averageResidenceTime;
    /**
     * The Strikler coefficient.
     */
    private double ks;
    /**
     * Minimum pipe slope.
     * <p>
     * The slope calculate to the program have to be grater than this.
     * </p>
     */
    private double minimumPipeSlope;
    /**
     * The height of the free surface at the begin of the pipe.
     */
    public double initialFreesurface;
    /**
     * The height of the free surface at the end of the pipe.
     */
    public double finalFreesurface;
    /**
     * Empty degree of the pipe.
     */
    public double emptyDegree;
    /**
     * The type of pipe.
     * 
     * <p>
     * It can be:
     * <ol>
     * <li>circular.
     * <li>rectangular.
     * <li>trapezium.
     * </ol>
     * </p>
     */
    private int pipeSectionType;
    /**
     * The depth of the dig at the end of the pipe.
     */
    public double depthFinalPipe;
    /**
     * The depth of the dig at the begin of the pipe.
     */
    public double depthInitialPipe;
    /**
     * Diameter of the pipe.
     */
    public double diameter;
    /**
     * The actually slope.
     */
    public double pipeSlope;
    /**
     * Time at the maximum discharge.
     */
    public double tQmax;
    /**
     * Discharge.
     */
    public double discharge;
    /**
     * Udometric coefficient.
     */
    public double coeffUdometrico;
    /**
     * Average slope of the pipes.
     */
    private double averageSlope;
    /**
     * Average speed into the pipe.
     */
    public double meanSpeed;
    /**
     * Residence time.
     */
    public double tP;
    /**
     * The effective diameter in the verify mode.
     * 
     */
    public double residenceTime;;

    /**
     * Pipe slope used to verify the net in mode 1.
     */
    public double verifyPipeSlope;
    /**
     * diameter of the pipe, used to verify the net in mode 1.
     */
    public double diameterToVerify;

    public double k;
    /**
     * The coordinate of the pipes extreme.
     */
    public Coordinate[] point = null;
    /**
     * The amount of the area which drain in this pipe, included all the upstream pipe.
     */
    public double totalSubNetArea;
    /**
     * The amount of the length of the upstream pipes.
     */
    public double totalSubNetLength;

    /**
     * Mean length of upstream net [ m ]
     */
    public double meanLengthSubNet;
    /**
     * Variance of lengths of upstream net [ m ^ 2 ]
     */
    public double varianceLengthSubNet;
    private static HortonMessageHandler msg = HortonMessageHandler.getInstance();
    private IHMProgressMonitor pm;
    public Pipe( PipeCombo pipeCombo, boolean isProject, boolean isAreaNotAllDry, IHMProgressMonitor pm ) throws Exception {
        this.pm = pm;
        setKnowNetworkValue(pipeCombo, isProject, isAreaNotAllDry);
    }

    public void setK( double defaultEsp1, double defaultExponent, double defaultGamma ) {
        this.k = (HOUR2MIN * averageResidenceTime * pow(drainArea / METER2CM, defaultExponent))
                / (pow(runoffCoefficient, defaultEsp1) * pow(averageSlope, defaultGamma));

    }

    public double getAverageSlope() {
        return averageSlope;
    }

    public void setAverageSlope( double averageSlope ) {
        this.averageSlope = averageSlope;
    }

    public double getAverageResidenceTime() {
        return averageResidenceTime;
    }

    public int getId() {
        return id;
    }

    public Integer getIdPipeWhereDrain() {
        return idPipeWhereDrain;
    }

    public void setIdPipeWhereDrain( int idPipeWhereDrain ) {
        this.idPipeWhereDrain = idPipeWhereDrain;
    }

    public double getDrainArea() {
        return drainArea;
    }

    public double getLenght() {
        return lenght;
    }

    public double getInitialElevation() {
        return initialElevation;
    }

    public double getFinalElevation() {
        return finalElevation;
    }

    public double getRunoffCoefficient() {
        return runoffCoefficient;
    }

    public double getKs() {
        return ks;
    }

    public double getMinimumPipeSlope() {
        return minimumPipeSlope;
    }

    public int getPipeSectionType() {
        return pipeSectionType;
    }

    public void setTolerance( double tolerance ) {
        this.tolerance = tolerance;
    }

    public double getTolerance() {
        return tolerance;
    }

    public void setAlign( int align ) {
        this.align = align;
    }

    public int getAlign() {
        return align;
    }

    public void setMaxJunction( int maxJunction ) {
        this.maxJunction = maxJunction;
    }

    public int getMaxJunction() {
        return maxJunction;
    }

    public void setMinDischarge( double minDischarge ) {
        this.minDischarge = minDischarge;
    }

    public double getMinDischarge() {
        return minDischarge;
    }

    public void setTau( double tau ) {
        this.tau = tau;
    }

    public double getTau() {
        return tau;
    }

    public void setG( double g ) {
        this.g = g;
    }

    public double getG() {
        return g;
    }

    public void setC( double c ) {
        this.c = c;
    }

    public double getC() {
        return c;
    }

    public double getMaxTheta() {
        return maxTheta;
    }

    public void setMaxTheta( double maxTheta ) {
        this.maxTheta = maxTheta;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public int getjMax() {
        return jMax;
    }

    public double getMinG() {
        return minG;
    }

    public void setAccuracy( double accuracy ) {
        this.accuracy = accuracy;
    }

    public void setMinimumDepth( double minimumDepth ) {
        this.minimumDepth = minimumDepth;
    }

    public void setJMax( int jMax ) {
        this.jMax = jMax;
    }

    public void setMinG( double minG ) {
        this.minG = minG;
    }

    /**
     * Set the value of the network.
     * 
     * <p>
     * Extract the needed value from a featureCollection.
     * </p>
     * 
     * @param pipeFeature
     *            the feature where the value are stored.
     * @param verify
     *            if the mode is project or verify.
     * @param isAreaNotAllDry 
     * @throws Exception
     *             if the featureCollection doesn't contains the field
     */
    private void setKnowNetworkValue( PipeCombo pipeCombo, boolean isProject, boolean isAreaNotAllDry ) throws Exception {

        try {
            
            SimpleFeature pipeFeature = pipeCombo.getPipeFeature();

            /*
             * Verifiche su ID e idPipeWhereDrain sono fatte durante la verifica
             * di consistenza della rete.
             */
            this.id = getAttribute(pipeFeature, PipesTrentoP.ID.getAttributeName()).intValue();
            double tmp = pipeCombo.getArea();
            if (tmp >= 0.0) {
                if (!isAreaNotAllDry) {
                    this.drainArea = tmp;
                } else {
                    double tmpPerc = getAttribute(pipeFeature, PipesTrentoP.PER_AREA.getAttributeName()).doubleValue();
                    if (tmpPerc >= 0 && tmpPerc <= 1) {
                        this.drainArea = tmp * tmpPerc;
                    } else {
                        pm.errorMessage(msg.message("trentoP.error.fieldPerArea") + tmpPerc);
                        throw new IllegalArgumentException(msg.message("trentoP.error.fieldPerArea" + tmpPerc));
                    }

                }
            } else {
                pm.errorMessage(msg.message("trentoP.error.fieldArea") + tmp);
                throw new IllegalArgumentException(msg.message("trentoP.error.fieldArea" + tmp));
            }
            this.lenght = ((Geometry) pipeFeature.getDefaultGeometry()).getLength();
            this.point = ((Geometry) pipeFeature.getDefaultGeometry()).getCoordinates();

            this.initialElevation = pipeCombo.getInitialJunctionElev(); // TODO also add depth?

            this.finalElevation = pipeCombo.getFinalJunctionElev(); // TODO also add depth?
            

            tmp = getAttribute(pipeFeature, PipesTrentoP.RUNOFF_COEFFICIENT.getAttributeName()).doubleValue();
            if (tmp >= 0) {
                this.runoffCoefficient = tmp;
            } else {
                pm.errorMessage(msg.message("trentoP.error.runO") + this.id);
                throw new IllegalArgumentException(msg.message("trentoP.error.runO" + this.id));
            }

            tmp = getAttribute(pipeFeature, PipesTrentoP.KS.getAttributeName()).doubleValue();
            if (tmp >= 0) {
                this.ks = tmp;
            } else {
                pm.errorMessage(msg.message("trentoP.error.ks") + this.id);
                throw new IllegalArgumentException(msg.message("trentoP.error.ks" + this.id));

            }
            this.averageSlope = getAttribute(pipeFeature, PipesTrentoP.AVERAGE_SLOPE.getAttributeName()).intValue();;

            tmp = getAttribute(pipeFeature, PipesTrentoP.AVERAGE_RESIDENCE_TIME.getAttributeName()).doubleValue();;

            if (tmp >= 0) {
                this.averageResidenceTime = tmp;
            } else {
                pm.errorMessage(msg.message("trentoP.error.averageTime") + this.id);
                throw new IllegalArgumentException(msg.message("trentoP.error.averageTime" + this.id));
            }
            if (!isProject) {
                /* Pipe diameter [cm] */

                this.diameterToVerify = getAttribute(pipeFeature, PipesTrentoP.DIAMETER.getAttributeName()).doubleValue();
                /* Pipe slope [%] */

                this.verifyPipeSlope = 100.0 * Math.abs(this.initialElevation - this.finalElevation) / this.lenght;

            } else {
                this.minimumPipeSlope = getAttribute(pipeFeature, PipesTrentoP.MINIMUM_PIPE_SLOPE.getAttributeName())
                        .doubleValue();

                int sectionTmp = getAttribute(pipeFeature, PipesTrentoP.PIPE_SECTION_TYPE.getAttributeName()).intValue();
                if (sectionTmp > 0 && sectionTmp < 4) {
                    this.pipeSectionType = sectionTmp;
                } else {
                    pm.errorMessage(msg.message("trentoP.error.section") + this.id);
                    throw new IllegalArgumentException(msg.message("trentoP.error.section") + this.id);
                }

            }

        } catch (NullPointerException e) {
            pm.errorMessage(msg.message("trentop.inputMatrix"));
            throw new Exception(msg.message("trentop.inputMatrix"));

        }

    }

    /**
     * Check if there is the field in a SimpleFeature and if it's a Number.
     * 
     * @param pipe
     *            the feature.
     * @param key
     *            the key string of the field.
     * @return the Number associated at this key.
     */
    private Number getAttribute( SimpleFeature pipe, String key ) {
        Number field = ((Number) pipe.getAttribute(key));
        if (field == null) {
            pm.errorMessage(msg.message("trentoP.error.number") + key);
            throw new IllegalArgumentException(msg.message("trentoP.error.number") + key);
        }

        // return the Number
        return field;
    }

    /**
     * Calculate the dimension of the pipes.
     * 
     * <p>
     * It switch between several section geometry.
     * </p>
     * 
     * 
     * @param diameters
     *            matrix with the commercial diameters.
     * @param tau
     *            tangential stress at the bottom of the pipe..
     * @param g
     *            fill degree.
     * @param maxd
     *            maximum diameter.
     * @param c
     *            is a geometric expression b/h where h is the height and b base
     *            (only for rectangular or trapezium shape).
     */

    public void designPipe( double[][] diameters, double tau, double g, double maxd, double c, StringBuilder strWarnings ) {

        switch( this.pipeSectionType ) {
        case 1:
            designCircularPipe(diameters, tau, g, maxd, strWarnings);
            break;
        case 2:
            designRectangularPipe(tau, g, maxd, c, strWarnings);
            break;
        case 3:
            designTrapeziumPipe(tau, g, maxd, c, strWarnings);
            break;
        default:
            designCircularPipe(diameters, tau, g, maxd, strWarnings);
            break;
        }
    }

    /**
     * Dimensiona la tubazione.
     * 
     * <p>
     * Chiama altre subroutine per il dimensionamento del diametro del tubo
     * circolare e calcola alcune grandezze correlate.
     * <ol>
     * <li>
     * Inanzittuto dimensiona il tubo chiamando la diameters. Quest'ultimasi
     * basa sul criterio di autopulizia della rete, e determina, mediante
     * chiamatead ulteriori funuzioni, tutte le grandezze necessarie (grado di
     * riempimento,angolo, ecc..)
     * <li>Dopodiche riprogetta il tratto (chiamata a get_diameter_i) imponendo
     * unapendenza pari a quella del terreno, qualora questa fosse superioe a
     * quellaprecedentemete calcolata.
     * <li>Inoltre ogni volta che dimensiona il tubo, design_pipe (o le altre
     * funzionida essa chiamate) calcola tutte le grandezze secondarie, ma
     * alquantofondamentali (quota dello scavo, pelo libero ecc..) e le registra
     * nellamatrice networkPipes.
     * <ol>
     * </p>
     * 
     * @param diameter
     *            matrice dei possibili diametri.
     * @param tau
     *            sforzo al fondo.
     * @param g
     *            grado di riempimento.
     * @param maxd
     *            massimo diametro
     * @param strWarnings
     *            a string which collect all the warning messages.
     */
    private void designCircularPipe( double[][] diameters, double tau, double g, double maxd, StringBuilder strWarnings )

    {
        /* Angolo di riempimento */
        double newtheta;
        /*
         * [%] Pendenza naturale, calcolata in funzione dei dati geometrici
         * della rete
         */
        double naturalslope;
        /* [cm] Diametro tubo */
        double D;
        /* [cm]Spessore tubo */
        double[] dD = new double[1];
        /* [%] Pendenza minima per il tratto che si sta dimensionando. */
        double ms;
        /*
         * Dimensiona il diametro del tubo da adottare col criterio di
         * autopulizia, calcola altre grandezze correlate come la pendenza e il
         * grado di riempimento, e restituisce il riempimento effettivo nel
         * tratto progettato
         */
        newtheta = getDiameter(diameters, tau, g, dD, maxd, strWarnings);
        /* [%] Pendenza del terreno */
        naturalslope = METER2CM * (getInitialElevation() - getFinalElevation()) / getLenght();
        /* [%] pendenza minima del tratto che si sta progettando. */
        ms = getMinimumPipeSlope();

        if (naturalslope < 0) {
            /*
             * Avvisa l'utente che il tratto che si sta progettando e in
             * contropendenza
             */
            strWarnings.append(msg.message("trentoP.warning.slope") + id);
        }
        /* La pendenza del terreno deve essere superiore a quella minima ms */
        if (naturalslope < ms) {
            naturalslope = ms;
        }
        /*
         * Se la pendenza del terreno e maggiore di quella del tubo, allora la
         * pendenza del tubo viene posta uguale a quella del terreno.
         */
        if (naturalslope > pipeSlope) {
            pipeSlope = (naturalslope);
            /*
             * Progetta la condotta assegnado una pendenza pari a quella del
             * terreno, calcola altre grandezze correlate come la pendenza e il
             * grado di riempimento, e restituisce il riempimento effettivo nel
             * tratto progettato
             */
            newtheta = getDiameterI(diameters, naturalslope, g, dD, maxd, strWarnings);

        }
        /* Diametro tubo [cm] */
        D = diameter;
        /* Velocita media nella sezione [m/s] */
        meanSpeed = ((8 * discharge) / (CUBICMETER2LITER * D * D * (newtheta - sin(newtheta)) / METER2CMSQUARED));
        /* Quota scavo all'inizio del tubo [m s.l.m.] */
        depthInitialPipe = (getInitialElevation() - minimumDepth - (D + 2 * dD[0]) / METER2CM);
        /*
         * Quota dello scavo alla fine del tubo calcolata considerando la
         * pendenza effettiva del tratto [m s.l.m.]
         */
        depthFinalPipe = (depthInitialPipe - getLenght() * pipeSlope / METER2CM);
        /* Quota pelo libero all'inizio del tubo [m s.l.m.] */
        initialFreesurface = depthInitialPipe + emptyDegree * D / METER2CM + dD[0] / METER2CM;
        /* Quota pelo libero all'inizio del tubo [m s.l.m.] */
        finalFreesurface = depthFinalPipe + emptyDegree * D / METER2CM + dD[0] / METER2CM;

    }

    /**
     * Dimensiona il tubo, imponendo uno sforzo tangenziale al fondo.
     * 
     * <p>
     * <ol>
     * <li>Calcola l'angolo theta in funzione di g.
     * <li>Nota la portata di progetto del tratto considerato, determina il
     * diametro oldD (adottando una pendenza che garantisca l'autopulizia).
     * <li>Successivamente oldD viene approssimato al diametro commerciale piu
     * vicino, letto dalla martice diametrs. Lo spessore adottato, anche esso
     * letto dalla matrice dei diametri, viene assegnato al puntatore dD. Mentre
     * la varabile maxd fa in modo che andando verso valle i diametri utilizzati
     * possano solo aumentare.
     * <li>A questo punto la get_diameter() ricalcola il nuovo valore
     * dell'angolo theta, chiamando la funzione this_bisection(), theta deve
     * risultare piu piccolo.
     * <li>Se invece oldD risulta maggiore del diametro commerciale piu grande
     * disponibile, allora si mantiene il suo valore.
     * <li>Infine calcola il grado di riempimento e pendenza del tratto a
     * partire dal raggio idraulico, e li registra nella matrice networkPipes.
     * <ol>
     * </p>
     * 
     * @param diametrs
     *            matrice che contiene i diametri e spessori commerciali.
     * @param tau
     *            [Pa] Sforzo tangenziale al fondo che garantisca l'autopulizia
     *            della rete
     * @param dD
     * @param g
     *            Grado di riempimento da considerare nella progettazione della
     *            rete
     * @param maxd
     *            Diamtetro o altezza piu' grande adottato nei tratti piu' a
     *            monte
     * @param strWarnings
     *            a string which collect all the warning messages.
     */

    private double getDiameter( double[][] diameters, double tau, double g, double[] dD, double maxd,
            StringBuilder strWarnings ) {
        /* Pari a A * ( Rh ^1/6 ) */
        double B;
        /* Anglo formato dalla sezione bagnata */
        double thta;
        /* Diametro calcolato imponendo il criterio di autopulizia della rete */
        double oldD;
        /* [cm]Diametro commerciale */
        double D = 0;
        /* Costane */
        double known;
        /*
         * [rad]Angolo formato dalla sezione bagnata, adottando un diametro
         * commerciale
         */
        double newtheta;
        /*
         * [cm] Nuovo raggio idraulico calcolato in funzione del diametro
         * commerciale
         */
        double newrh;
        /* B=A(Rh^1/6) [m^13/6] */
        B = (discharge * sqrt(WSPECIFICWEIGHT / tau)) / (CUBICMETER2LITER * getKs());
        /* Angolo formato dalla sezione bagnata [rad] */
        thta = 2 * acos(1 - 2 * g);
        /* Diametro tubo [cm] */
        oldD = TWO_TWENTYOVERTHIRTEEN * pow(B, SIXOVERTHIRTEEN)
                / (pow((1 - sin(thta) / thta), ONEOVERTHIRTEEN) * pow(thta - sin(thta), SIXOVERTHIRTEEN));
        /*
         * Se il diametro ottenuto e piu piccolo del diametro commerciale piu
         * grande, allora lo approssimo per eccesso col diametro commerciale piu
         * prossimo.
         */
        if (oldD < diameters[diameters.length - 1][0]) {
            int j = 0;
            for( j = 0; j < diameters.length; j++ ) {
                /* Diametro commerciale [cm] */
                D = diameters[j][0];

                if (D >= oldD)
                    break;
            }

            if (D < maxd) {
                /*
                 * Scendendo verso valle i diametri usati non possono diventare
                 * piu piccoli
                 */
                D = maxd;
            }

            diameter = (D);
            /* Spessore corrispondente al diametro commerciale scelto [cm] */
            dD[0] = diameters[j][1];

            known = (B * TWO_TENOVERTHREE) / pow(D / METER2CM, THIRTHEENOVERSIX);
            /*
             * Angolo formato dalla sezione bagnata considerando un diametro
             * commerciale [rad]
             */
            newtheta = Utility.thisBisection(thta, known, ONEOVERSIX, minG, accuracy, jMax, pm, strWarnings);
            /*
             * E ovvio che adottando un diametro commerciale piu grande di
             * quello che sarebbe strettamente neccessario, il grado di
             * riempimento non puo aumentare
             */
            if (newtheta > thta) {
                strWarnings.append(msg.message("trentoP.warning.bisection") + id);
            }

        }
        /*
         * se il diametro necessario e piu grande del massimo diametro
         * commerciale disponibile, allora mantengo il risultato ottenuto senza
         * nessuna approssimazione
         */
        else {

            D = oldD;
            diameter = D; /* COSA SUCCEDE ALLO SPESSORE ??!! */
            newtheta = thta;
        }
        /* Grado di riempimento del tubo */
        emptyDegree = 0.5 * (1 - cos(newtheta / 2));
        /* Rh [cm] */
        newrh = 0.25 * D * (1 - sin(newtheta) / newtheta);
        /* pendenza del tratto progettato [%] */
        pipeSlope = (tau / (WSPECIFICWEIGHT * newrh) * METER2CMSQUARED);

        return newtheta;

    }

    /**
     * 
     * Dimensiona il tubo adottando una pendenza pari a quella del terreno/
     * <p>
     * Sostanzialmente uguale alla get_diameter. L'unica differenza e che
     * dimensiona il tubo adottando una pendenza pari a quella del terreno.
     * </p>
     * 
     * 
     * @param diametrs
     *            matrice che contiene i diametri e spessori commerciali.
     * @param slope
     *            pendenza della tubazione.
     * @param dD
     *            spessore del tubo commerciale.
     * @param g
     *            Grado di riempimento da considerare nella progettazione della
     *            rete
     * @param maxd
     *            Diamtetro o altezza piu' grande adottato nei tratti piu' a
     *            monte
     * @param strWarnings
     *            a string which collect all the warning messages.
     */
    private double getDiameterI( double[][] diameters, double slope, double g, double[] dD, double maxd,
            StringBuilder strWarnings ) {
        int j; /* \param */
        /* Pari a A * ( Rh ^1/6 ) */
        double B;
        /* Anglo formato dalla sezione bagnata */
        double thta;
        /* Diametro calcolato imponendo il criterio di autopulizia della rete */
        double oldD;
        /* [cm]Diametro commerciale */
        double D = 0;
        /* Costane */
        double known;
        /*
         * [rad]Angolo formato dalla sezione bagnata, adottando un diametro
         * commerciale
         */
        double newtheta;
        /*
         * B = A ( Rh ^ 2 / 3 ) [ m ^ 8 / 3 ]
         */
        B = (discharge) / (CUBICMETER2LITER * getKs() * sqrt(slope / METER2CM));
        /*
         * Angolo formato dalla sezione bagnata [rad]
         */

        thta = 2 * acos(1 - 2 * g);
        /*
         * Diametro tubo [ cm ]
         */
        oldD = TWO_THIRTEENOVEREIGHT * pow(B, THREEOVEREIGHT)
                / (pow((1 - sin(thta) / thta), 0.25) * pow(thta - sin(thta), THREEOVEREIGHT));

        if (oldD < diameters[diameters.length - 1][0]) {
            for( j = 0; j < diameters.length; j++ ) {
                D = diameters[j][0]; /* Diametro commerciale del tubo [cm] */

                if (D >= oldD)
                    break;
            }

            if (j == diameters.length + 1 || j == diameters.length) {

                strWarnings.append(msg.message("trentoP.warning.loop"));
                j = diameters.length - 1;
            }

            if (D < maxd) {
                /*
                 * Il diametro adottando non puo decrescere procedendo verso
                 * valle
                 */
                D = maxd;
            }

            diameter = D;
            /* Spessore tubo commerciale [cm] */
            dD[0] = diameters[j][1];

            /*
             * B=(networkPipes->element[l][10])/(CUBICMETER2LITER*networkPipes->
             * element[l][ 9]*.sqrt(slope/METER2CM));
             */
            known = (B * TWO_THIRTEENOVERTHREE) / pow(D / METER2CM, EIGHTOVERTHREE);
            /* Riempimento effettivo considerando il diametro commerciale [rad] */
            newtheta = Utility.thisBisection(thta, known, TWOOVERTHREE, minG, accuracy, jMax, pm, strWarnings);

            if (newtheta > thta) {
                strWarnings.append(msg.message("trentoP.warning.bisection") + id);
            }

        } else {
            D = oldD;
            diameter = D;
            newtheta = thta;
        }
        /* Grdao di riempimento */
        emptyDegree = 0.5 * (1 - cos(newtheta / 2));
        /* Pendenza tubo [%] */
        pipeSlope = slope;

        return newtheta;

    }

    /**
     * Dimensiona una sezione di tipo 2, ossia rettangolare.
     * <p>
     * Inoltre calcola altre grndezze correlate.
     * <ol>
     * <li>
     * Inizialmente dimensiona il tubo chiamando la get_height_1() la quale
     * adotta il criterio dell'autopulizia e determina, mediante chiamate ad
     * ulteriori funuzioni tutte le grandezze necessarie (grado di riempimento,
     * pendenza, ecc..)
     * <li>Dopodiche riprogetta il tratto (chiamata a get_height_1_i) imponendo
     * una pendenza pari a quella del terreno, qualora questa fosse superioe a
     * quella ottenuta secondo il criterio di autopulizia.
     * <li>Inoltre ogni volta che dimensiona il tubo, design_pipe_2 (o le altre
     * funzioni da essa chiamate) calcola tutte le grandezze secondarie, ma
     * alquanto fondamentali (quota dello scavo, pelo libero ecc..) e le
     * registra nella matrice networkPipes.
     * </ol>
     * </p>
     * 
     * @param diametrs
     *            matrice che contiene i diametri e spessori commerciali.
     * @param tau
     *            [Pa] Sforzo tangenziale al fondo che garantisca l'autopulizia
     *            della rete
     * @param g
     *            Grado di riempimento da considerare nella progettazione della
     *            rete
     * @param maxd
     *            Diamtetro o altezza piu' grande adottato nei tratti piu' a
     *            monte
     * @param c
     *            Rapporto base-altezza della sezione rettangolare.
     * @param strWarnings
     *            a string which collect all the warning messages.
     */
    private void designRectangularPipe( double tau, double g, double maxd, double c, StringBuilder strWarnings ) {
        /* [cm] base della sezione effettivamente adottata. */
        double base;
        /*
         * [%] pendenza naturale, calcolata in funzione dei dati geometrici
         * della rete
         */
        double naturalslope;
        /* D [cm] Altezza del canale a sezione rettangolare. */
        double D;
        /*
         * [%] Pendenza minima da adottare per il tratto che si sta
         * dimensionando.
         */
        double ms;

        /*
         * Dimensiona il diametro del tubo da adottare col criterio di
         * autopulizia, calcola altre grandezze correlate come la pendenza e il
         * grado di riempimento, e restituisce il riempimento effettivo nel
         * tratto progettato
         */
        base = getHeight1(tau, g, maxd, c);

        /* [%] pendenza minima per il tratto che si sta dimensionando. */
        ms = getMinimumPipeSlope();
        /* [%] Pendenza del terreno */
        naturalslope = METER2CM * (getInitialElevation() - getFinalElevation()) / getLenght();
        /*
         * Avvisa l'utente che il tratto che si sta progettando e in
         * contropendenza
         */
        if (naturalslope < 0) {
            strWarnings.append(msg.message("trentoP.warning.slope") + id);
        }
        /* La pendenza del terreno deve essere superiore a quella minima ms */
        if (naturalslope < ms) {
            naturalslope = ms;
        }
        /*
         * Se la pendenza del terreno e maggiore di quella del tubo, allora la
         * pendenza del tubo viene posta uguale a quella del terreno.
         */
        if (naturalslope > pipeSlope) {
            pipeSlope = naturalslope;
            /*
             * Progetta la condotta assegnado una pendenza pari a quella del
             * terreno, calcola altre grandezze correlate come la pendenza e il
             * grado di riempimento, e restituisce il riempimento effettivo nel
             * tratto progettato
             */
            base = getHeight1I(naturalslope, g, maxd, c);

        }
        /* Diametro tubo [cm] */
        D = diameter;
        /* Velocita media nella sezione [m/s] */
        meanSpeed = (discharge / CUBICMETER2LITER) / (emptyDegree * D * base / METER2CMSQUARED);
        /* Quota scavo all'inizio del tubo [m s.l.m.] */
        depthInitialPipe = initialElevation - diameter / METER2CM;
        /*
         * Quota dello scavo alla fine del tubo calcolata considerando la
         * pendenza effettiva del tratto [m s.l.m.]
         */
        depthFinalPipe = depthInitialPipe - getLenght() * pipeSlope / METER2CM;
        /* Quota pelo libero all'inizio del tubo [m s.l.m.] */
        initialFreesurface = depthInitialPipe + emptyDegree * D / METER2CM;
        /* Quota pelo libero all'inizio del tubo [m s.l.m.] */
        finalFreesurface = depthFinalPipe + emptyDegree * D / METER2CM;

    }

    /**
     * 
     * Dimensiona il canale a sezione rettangolare, imponendo uno sforzo
     * tangenziale al fondo.
     * 
     * <p>
     * <ol>
     * <li>Calcola l'altezza oldD della sezione rettangolare in funzione del
     * grado di riempimento e del rapporto base-altezza assegnati; ovviamente
     * imponendo un certo valore dello sforzo tangenziale al fondo (criterio di
     * autopulizia della rete.)
     * <li>Calcola la base oldb.
     * <li>Calcola il raggio idraulico richiesto, e a partire da esso determina
     * sia il grado di riempimento nella sezione commerciale che la pendenza
     * della tubazione, e gli registra nella matrice networkPipes.
     * <li>Infine la get_height_1 restituisce il valore della base della sezione
     * rettangolare adottata.
     * 
     * @param tau
     *            [Pa] Sforzo tangenziale al fondo che garantisca l'autopulizia
     *            della rete
     * @param g
     *            Grado di riempimento da considerare nella progettazione della
     *            rete
     * @param maxd
     *            Diamtetro o altezza piu' grande adottato nei tratti piu' a
     *            monte
     * @param c
     *            Rapporto base-altezza della sezione rettangolare.
     */
    private double getHeight1( double tau, double g, double maxd, double c ) {

        /* Pari a A * ( Rh ^1/6 ) */
        double B;
        /* [cm] Altezza della sezione rettangolare effettivamente impiegata. */
        double oldD;
        /* [cm] Altezza della sezione rettangolare effettivamente impiegata. */
        double D = 0;
        /* [cm] la base della sezione rettangolare. */
        double oldb;
        /* [cm] La base della sezione effettivamente usata. */
        double b;
        /* [cm]il raggio idraulico. */
        double rh;
        /* B=A(Rh^1/6) [m^13/6] */
        B = (discharge * sqrt(WSPECIFICWEIGHT / tau)) / (CUBICMETER2LITER * ks);
        /* altezza sezione rettangolare */
        oldD = METER2CM * pow(B, SIXOVERTHIRTEEN) * pow((2 * g + c), ONEOVERTHIRTEEN) / pow((g * c), SEVENOVERTHIRTEEN);
        /* [cm] base */
        oldb = c * oldD;
        D = ceil(oldD);
        b = c * D;

        diameter = D;
        /* Rh [cm] raggio idraulico. */
        rh = g * oldD * oldb / (2 * g * oldD + oldb);
        /* Grado di riempimento del tubo */
        emptyDegree = b * rh / (D * (b - 2 * rh));
        /* pendenza del tratto progettato [%] */
        pipeSlope = (tau / (WSPECIFICWEIGHT * rh) * METER2CMSQUARED);

        return b;

    }

    /**
     * Dimensiona il tubo adottando una pendenza pari a quella del terreno
     * <p>
     * Sostanzialmente uguale alla get_height_1. L'unica differenza e che
     * dimensiona il tubo adottando una pendenza pari a quella del terreno.
     * <p>
     * 
     * @param slope
     *            , pendenza della tubazione.
     * @param g
     *            Grado di riempimento da considerare nella progettazione della
     *            rete
     * @param maxd
     *            Diamtetro o altezza piu' grande adottato nei tratti piu' a
     *            monte
     * @param c
     *            Rapporto base-altezza della sezione rettangolare.
     */
    private double getHeight1I( double slope, double g, double maxd, double c ) {
        /* B Pari a A * ( Rh^ 1/6) */
        double B;
        /*
         * [cm] Diametro calcolato adottando una pendenza per i canali pari a
         * quella del terreno.
         */
        double oldD;
        /* [cm] Altezza della sezione trapezoidale. */
        double D;
        /* [cm] Base della sezione trapezoidale. */
        double oldb;
        /* [cm] Base della sezione trapezoidale finale. */
        double b;

        double rh;

        /* B=A(Rh^2/3) [m^13/6] */
        B = (discharge) / (CUBICMETER2LITER * ks * sqrt(slope / METER2CM));
        /* altezza sezione rettangolare */
        oldD = METER2CM * pow(B, THREEOVEREIGHT) * pow(2 * g + c, ONEOVERFOUR) / pow((g * c), FIVEOVEREIGHT);
        /* [cm] base */
        oldb = c * oldD;
        D = ceil(oldD);
        b = c * D;

        diameter = D;
        /* Rh [cm] raggio idraulico. */
        rh = g * oldD * oldb / (2 * g * oldD + oldb);
        /* Grado di riempimento del tubo */
        emptyDegree = b * rh / (D * (b - 2 * rh));
        /* pendenza del tratto progettato [%] */
        pipeSlope = slope;

        return b;

    }

    /**
     * Chiama altre subroutine per dimensionare una sezione di tipo 3, ossia
     * trapezioidale.
     * 
     * <p>
     * Oltre al dimensionamento vero e proprio, calcola anche tutte le altre
     * grandezze correlate.
     * <ol>
     * <li>Inizialmente dimensiona il tubo chiamando la get_height_2() la quale
     * adotta il criterio dell'autopulizia e determina, mediante chiamate ad
     * ulteriori funuzioni tutte le grandezze necessarie (grado di riempimento,
     * pendenza, ecc..)
     * <li>Dopodiche riprogetta il tratto (chiamata a get_height_2_i) imponendo
     * una pendenza pari a quella del terreno, qualora questa fosse superioe a
     * quella ottenuta secondo il criterio di autopulizia.
     * <li>Inoltre ogni volta che dimensiona il tubo, design_pipe_3 (o le altre
     * funzioni da essa chiamate) calcola tutte le grandezze secondarie, ma
     * alquanto fondamentali (quota dello scavo, pelo libero ecc..) e le
     * registra nella matrice networkPipes.
     * </ol>
     * </p>
     * 
     * @param tau
     *            [Pa] Sforzo tangenziale al fondo che garantisca l'autopulizia
     *            della rete
     * @param g
     *            Grado di riempimento da considerare nella progettazione della
     *            rete
     * @param maxd
     *            Diamtetro o altezza piu' grande adottato nei tratti piu' a
     *            monte
     * @param c
     *            Rapporto base-altezza della sezione rettangolare.
     */

    private void designTrapeziumPipe( double tau, double g, double maxd, double c, StringBuilder strWarnings ) {
        /* [cm] Base della sezione effettivamente adottata. */
        double base;
        /*
         * [%] Pendenza naturale, calcolata in funzione dei dati geometrici
         * della rete
         */
        double naturalSlope;
        /* [cm] Altezza del canale trapezoidale. */
        double D;
        /*
         * [%] Pendenza minima da adottare per il tratto che si sta
         * dimensionando.
         */
        double ms;

        /* DUE FORMULE */

        /*
         * Dimensiona il diametro del tubo da adottare col criterio di
         * autopulizia, calcola altre grandezze correlate come la pendenza e il
         * grado di riempimento, e restituisce il riempimento effettivo nel
         * tratto progettato
         */
        base = getHeight2(tau, g, maxd, c);

        /* [%] pendenza minima per il tratto che si sta dimensionando. */
        ms = getMinimumPipeSlope();
        /* [%] Pendenza del terreno */
        naturalSlope = METER2CM * (initialElevation - finalElevation) / lenght;

        /*
         * Avvisa l'utente che il tratto che si sta progettando e in
         * contropendenza
         */
        if (naturalSlope < 0) {
            strWarnings.append(msg.message("trentoP.warning.slope") + id);
        }
        /* La pendenza del terreno deve essere superiore a quella minima ms */
        if (naturalSlope < ms) {
            naturalSlope = ms;
        }
        /*
         * Se la pendenza del terreno e maggiore di quella del tubo, allora la
         * pendenza del tubo viene posta uguale a quella del terreno.
         */
        if (naturalSlope > pipeSlope) {
            pipeSlope = naturalSlope;
            /*
             * Progetta la condotta assegnado una pendenza pari a quella del
             * terreno, calcola altre grandezze correlate come la pendenza e il
             * grado di riempimento, e restituisce il riempimento effettivo nel
             * tratto progettato
             */
            base = getHeight2I(naturalSlope, g, maxd, c);
        }
        /* Diametro tubo [cm] */
        D = diameter;

        // Velocita media nella sezione [ m / s ]
        meanSpeed = (discharge / CUBICMETER2LITER) / (emptyDegree * D * base / METER2CMSQUARED);
        // Quota scavo all 'inizio del tubo [ m s . l . m . ]
        depthInitialPipe = getInitialElevation() - diameter / METER2CM;
        /*
         * Quota dello scavo alla fine del tubo calcolata considerando la
         * pendenza effettiva del tratto [ m s . l . m . ]
         */
        depthFinalPipe = depthInitialPipe - getLenght() * pipeSlope / METER2CM;
        // Quota pelo libero all 'inizio del tubo [ m s . l . m . ]
        initialFreesurface = depthInitialPipe + emptyDegree * D / METER2CM;
        // Quota pelo libero all 'inizio del tubo [ m s . l . m . ]
        finalFreesurface = depthFinalPipe + emptyDegree * D / METER2CM;

    }

    /**
     * 
     * Dimensiona i canali trapezioidale, imponendo un certo sforzo tangenziale
     * al fondo.
     * <p>
     * <ol>
     * <li>Calcola l'altezza oldD della sezione trapezoidale in funzione del
     * grado di riempimento e del rapporto base-altezza assegnati; ovviamente
     * imponendo un certo valore dello sforzo tangenziale al fondo (criterio di
     * autopulizia della rete.)
     * <li>Calcola la base oldb.
     * <li>Calcola il raggio idraulico richiesto, e a partire da esso determina
     * sia il grado di riempimento nella sezione commerciale che la pendenza
     * della tubazione, e gli registra nella matrice networkPipes.
     * <li>Infine la get_height_2 restituisce il valore della base della sezione
     * rettangolare adottata.
     * </ol>
     * </l>
     * 
     * @param tau
     *            [Pa] Sforzo tangenziale al fondo che garantisca l'autopulizia
     *            della rete
     * @param g
     *            Grado di riempimento da considerare nella progettazione della
     *            rete
     * @param maxd
     *            [cm] Diamtetro o altezza piu' grande adottati nei tratti piu'
     *            a monte
     * @param c
     *            Rapporto tra base e altezza nella sezione trapezoidale.
     */
    private double getHeight2( double tau, double g, double maxd, double c ) {
        /* B Pari a A * ( Rh^ 1/6 ) */
        double B;
        /*
         * oldD [cm] Diametro calcolato imponendo il criterio di autopulizia
         * della rete
         */
        double oldD;
        /* D [cm] Altezza della sezione trapezoidale. */
        double D;
        /* oldb [cm] Base della sezione tapezoidale. */
        double oldb;
        /* b [cm] Base della sezione commerciale. */
        double b;
        /* newrh [cm] Raggio idraulico. */
        double rh;

        /* B=A(Rh^1/6) [m^13/6] */
        B = (discharge * sqrt(WSPECIFICWEIGHT / tau)) / (CUBICMETER2LITER * ks);
        /* [cm] altezza sezione trapezia */
        oldD = METER2CM * pow(B, SIXOVERTHIRTEEN) * pow((c + 2 * sqrt(2) * g), ONEOVERTHIRTEEN)

                / pow((g * (g + c)), SEVENOVERTHIRTEEN);

        /* [cm] base */
        oldb = c * oldD;
        D = ceil(oldD);
        b = c * D;

        diameter = D;
        /* Rh [cm] raggio idraulico. */
        rh = g * oldD * (g * oldD + oldb) / (oldb + 2 * sqrt(2) * g * oldD);
        /* Grado di riempimento del tubo */
        emptyDegree = ((2 * sqrt(2) * rh * D - D * b) + sqrt(pow((D * b - 2 * sqrt(2) * rh * D), 2) + 4 * D * D * rh * b))
                / (2 * D * D);
        /* pendenza del tratto progettato [%] */
        pipeSlope = (tau / (WSPECIFICWEIGHT * rh) * METER2CMSQUARED);

        return b;

    }

    /**
     * Dimensiona il tubo adottando una pendenza pari a quella del terreno
     * <p>
     * Sostanzialmente uguale alla get_height_2. L'unica differenza e che
     * dimensiona iltubo adottando una pendenza pari a quella del terreno.
     * </p>
     * 
     * @param slope
     *            [%] Pendenza del tratto da dimensionare, che e' pari a quella
     *            del terreno
     * @param g
     *            Grado di riempimento da considerare nella progettazione della
     *            rete
     * @param maxd
     *            [cm]Diamtetro o altezza piu' grande adottati nei tratti piu' a
     *            monte
     * @param c
     *            Rapporto tra base e altezza nella sezione trapezoidale.
     */
    private double getHeight2I( double slope, double g, double maxd, double c ) {
        /* B Pari a A * ( Rh^ 1/6 ). */
        double B;
        /*
         * oldD [cm] Altezza della sezione trapezoidale calcolato imponendo una
         * pendenza pari a quella del terreno.
         */
        double oldD;
        /* D [cm] Altezza della sezione trapezoidale. */
        double D;
        /* oldb [cm]Base della sezione trapezoidale. */
        double oldb;
        /* b [cm] la Base della sezione trapezoidale finale. */
        double b;
        /* newrh [cm] Raggio idraulico. */
        double rh;

        /* B=A(Rh^2/3) [m^13/6] */
        B = (discharge) / (CUBICMETER2LITER * ks * sqrt(slope / METER2CM));
        /* altezza sezione rettangolare */
        oldD = METER2CM * pow(B, THREEOVEREIGHT) * pow((c + 2 * sqrt(2) * g), ONEOVERFOUR) / pow((g * (g + c)), FIVEOVEREIGHT);
        /* [cm] base */
        oldb = c * oldD;
        D = ceil(oldD);
        b = c * D;

        diameter = D;
        /* Rh [cm] raggio idraulico. */
        rh = g * oldD * (g * oldD + oldb) / (oldb + 2 * sqrt(2) * g * oldD);
        /* Grado di riempimento del tubo */
        emptyDegree = ((2 * sqrt(2) * rh * D - D * b) + sqrt(pow((D * b - 2 * sqrt(2) * rh * D), 2) + 4 * D * D * rh * b))
                / (2 * D * D);
        /* pendenza del tratto progettato [%] */
        pipeSlope = slope;

        return b;
    }

    /** Verify if the empty degree is greather than the 0.8.
     * @param strWarnings
     *            a string which collect all the warning messages.
     *   @param q discharge in this pipe.                
     */
    public double verifyEmptyDegree( StringBuilder strWarnings, double q ) {
        /* Pari a A * ( Rh ^1/6 ) */
        double B;
        /* Anglo formato dalla sezione bagnata */
        double thta;
        /* Costante */
        double known;
        /*
         * [rad]Angolo formato dalla sezione bagnata, adottando un diametro
         * commerciale
         */
        double newtheta = 0;

        B = (q * sqrt(WSPECIFICWEIGHT / 2.8)) / (CUBICMETER2LITER * getKs());
        /* Angolo formato dalla sezione bagnata [rad] */
        thta = 2 * acos(1 - 2 * 0.8);
        known = (B * TWO_TENOVERTHREE) / pow(diameterToVerify / METER2CM, THIRTHEENOVERSIX);
        /*
         * Angolo formato dalla sezione bagnata considerando un diametro
         * commerciale [rad]
         */

        newtheta = Utility.thisBisection(thta, known, ONEOVERSIX, minG, accuracy, jMax, pm, strWarnings);

        /* Grado di riempimento del tubo */
        emptyDegree = 0.5 * (1 - cos(newtheta / 2));
        return newtheta;

    }

    public void setIndexPipeWhereDrain( Integer indexPipeWhereDrain ) {
        this.indexPipeWhereDrain = indexPipeWhereDrain;
    }

    public Integer getIndexPipeWhereDrain() {
        return indexPipeWhereDrain;
    }
}