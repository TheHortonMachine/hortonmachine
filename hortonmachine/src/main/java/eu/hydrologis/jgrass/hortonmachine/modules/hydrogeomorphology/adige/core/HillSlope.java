/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.hortonmachine.modules.hydrogeomorphology.adige.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import eu.hydrologis.jgrass.jgrassgears.libs.exceptions.ModelsIllegalargumentException;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.IHMProgressMonitor;

/**
 * The hilslope area, related to a particolar network link.
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class HillSlope implements Comparator<HillSlope> {
    private static Evapotranspiration evapTransCalculator = new Evapotranspiration();

    private int hillslopeId = -1;

    private SimpleFeature hillslopeFeature = null;
    private SimpleFeature linkFeature = null;
    private PfafstetterNumber pfafstetterNumber = null;

    private final List<HillSlope> upstreamElements = new ArrayList<HillSlope>();
    private HillSlope downstreamElement = null;

    private HillSlope firstOfMaiorBasin = null;
    private Geometry totalGeometryUpstream;
    private Random rn;
    private double hillslopeArea = -1;
    private double hillslopeUpstreamArea = -1;
    private double linkLength = -1;
    private double linkSlope = -1;

    private double baricenterElevation = -1;

    private int baricenterElevationAttributeIndex = -1;
    private int linkStartElevationAttributeIndex = -1;
    private int linkEndElevationAttributeIndex = -1;

    public final Parameters parameters;

    private boolean hasVegetation = false;
    private final int vegetationIdFieldIndex;

    public HillSlope( SimpleFeature netFeature, SimpleFeature basinFeature,
            PfafstetterNumber pfafNumber, int hillslopeId, int baricenterElevationFieldIndex,
            int linkStartElevationFieldIndex, int linkEndElevationFieldIndex,
            int vegetationIdFieldIndex ) {
        this.hillslopeId = hillslopeId;
        this.hillslopeFeature = basinFeature;
        this.linkFeature = netFeature;
        this.pfafstetterNumber = pfafNumber;
        this.baricenterElevationAttributeIndex = baricenterElevationFieldIndex;
        this.linkStartElevationAttributeIndex = linkStartElevationFieldIndex;
        this.linkEndElevationAttributeIndex = linkEndElevationFieldIndex;
        this.vegetationIdFieldIndex = vegetationIdFieldIndex;

        if (baricenterElevationAttributeIndex == -1) {
            throw new IllegalArgumentException("The baricenter field index can't be -1.");
        }
        // HashMap<Integer, Double> laiMap,
        // HashMap<Integer, Double> displacementMap,
        // HashMap<Integer, Double> roughnessMap,
        // double RGL, double ra, double rs, double rarc
        parameters = new Parameters();

        rn = new Random();
    }

    public int getHillslopeId() {
        return hillslopeId;
    }

    public SimpleFeature getLinkFeature() {
        return linkFeature;
    }

    /**
     * @return the length of the current hillslope's link. Dimension is meters.
     */
    public double getLinkLength() {
        if (linkLength == -1) {
            linkLength = ((Geometry) linkFeature.getDefaultGeometry()).getLength(); // [m]
        }
        return linkLength;
    }

    /**
     * @return the slope of the current hillslope's link. The result is the tangent.
     */
    public double getLinkSlope() {
        if (linkSlope == -1) {
            // hillslopeFeature.getAttribute(baricenterElevationAttribute);
            double startElev = (Double) linkFeature.getAttribute(linkStartElevationAttributeIndex);
            double endElev = (Double) linkFeature.getAttribute(linkEndElevationAttributeIndex);
            linkSlope = (startElev - endElev) / getLinkLength();

            if (linkSlope <= 0) {
                /*
                 * if < 0 then probably it is very flat and the dem si not precise. The slope is
                 * set.
                 */
                linkSlope = 0.001;
            }
        }
        return linkSlope;
    }

    /**
     * Assigns the channel widths of the links using a power law.
     * Width=coefficient*UpstreamArea[km2]^exponent+NORM(sdResiduals) where NORM() is a normally
     * distributed random variable.
     * 
     * @param coefficient The coefficient in the power law
     * @param exponent The exponent in the power law
     * @param sdResiduals The standard deviation of the residuals of the power law
     */
    public double getLinkWidth( double coefficient, double exponent, double sdResiduals ) {
        // Returns a random value that follows a gaussian distribution
        double sampleGaussian = rn.nextGaussian() * sdResiduals;
        double upstreamArea = getUpstreamArea(null) / 1000000.0;

        double width = coefficient * Math.pow(upstreamArea, exponent) * Math.exp(sampleGaussian);
        return width;
    }

    /**
     * Assigns the Chezy coefficient of the links using a power law.
     * Chezi=coefficient*LinkSlope^exponent+NORM(sdResiduals) where NORM() is a normally distributed
     * random variable.
     * 
     * @param coefficient The coefficient in the power law
     * @param exponent The exponent in the power law
     */
    public double getLinkChezi( double coefficient, double exponent ) {
        double chezi = coefficient * Math.pow(getLinkSlope(), exponent);
        return chezi;
    }

    public SimpleFeature getHillslopeFeature() {
        return hillslopeFeature;
    }

    /**
     * @return the area of the current hillslope. Dimension is meter^2
     */
    public double getHillslopeArea() {
        if (hillslopeArea == -1) {
            hillslopeArea = ((Geometry) hillslopeFeature.getDefaultGeometry()).getArea(); // m^2
        }
        return hillslopeArea;
    }

    public double getBaricenterElevation() {
        if (baricenterElevation == -1) {
            baricenterElevation = (Double) hillslopeFeature
                    .getAttribute(baricenterElevationAttributeIndex);
        }
        return baricenterElevation;
    }

    /**
     * @return the closure coordinate of the basin, i.e. the last coordinate of the river
     */
    public Coordinate getHillslopeClosure() {
        Coordinate[] coords = ((Geometry) linkFeature.getDefaultGeometry()).getCoordinates();
        return coords[coords.length - 1];
    }

    /**
     * Get the geometry from the actual point to the passed numbers of pfafstetter
     * 
     * @param limit
     * @param pm
     * @param doMonitor
     * @return
     */
    public Geometry getGeometry( List<PfafstetterNumber> limit, IHMProgressMonitor pm,
            boolean doMonitor ) {

        if (limit == null && totalGeometryUpstream != null) {
            return totalGeometryUpstream;
        }

        List<Geometry> geometries = new ArrayList<Geometry>();
        geometries.add((Geometry) hillslopeFeature.getDefaultGeometry());
        getAllUpstreamElementsGeometries(geometries, limit, this);
        GeometryFactory gFactory = new GeometryFactory();
        /*
         * join the geoms to a single one
         */
        Geometry runningGeometry = geometries.get(0);
        if (doMonitor)
            pm.beginTask("Estrazione geometrie dei bacini elementari a monte",
                    geometries.size() - 1);
        for( int i = 1; i < geometries.size(); i++ ) {
            if (doMonitor) {
                pm.worked(1);
            } else {
                pm.subTask("Unione geometrie " + i + "/" + (geometries.size() - 1));
            }
            List<Geometry> tmp = new ArrayList<Geometry>(2);
            tmp.add(runningGeometry);
            tmp.add(geometries.get(i));
            Geometry gCollection = gFactory.buildGeometry(tmp);
            runningGeometry = gCollection.buffer(0.0);
        }
        pm.subTask("");
        if (doMonitor)
            pm.done();

        // keep the total geometry, in case it is asked again
        if (limit == null) {
            totalGeometryUpstream = runningGeometry;
        }

        return runningGeometry;
        // return gCollection.buffer(0.0);
    }

    /**
     * Calculate the upstream area of the current hillslope.
     * 
     * @param limit a list of Pfafstetter numbers that define a list of hillslopes that block the
     *        recursion for area calculation. Through that for example we can define areas between
     *        two hillslopes.
     * @return the upstream area
     */
    public double getUpstreamArea( List<PfafstetterNumber> limit ) {
        if (hillslopeUpstreamArea == -1) {

            List<HillSlope> basins = new ArrayList<HillSlope>();
            getAllUpstreamElements(basins, limit);

            hillslopeUpstreamArea = 0;
            for( HillSlope elementarBasin : basins ) {
                hillslopeUpstreamArea = hillslopeUpstreamArea + elementarBasin.getHillslopeArea();
            }
        }
        return hillslopeUpstreamArea;
    }

    public PfafstetterNumber getPfafstetterNumber() {
        return pfafstetterNumber;
    }

    public HillSlope getFirstOfMaiorBasinElement() {
        return firstOfMaiorBasin;
    }

    // public UpstreamElement[] getHeadElements() {
    //
    // return null;
    // }
    //
    // public UpstreamElement getStartElement() {
    // UpstreamElement ue = getConnectedDownstreamElement().getStartElement();
    // if (ue == null) {
    // return this;
    // }
    // return ue;
    // }

    /**
     * Tries to add an element upstream to the actual one. A check is done on Pfafstetter to
     * understand if the passed element really is connected to the actual one. If it isn't the
     * element isn't added.
     * <p>
     * <b>Don't use this, this should usually be called only by:
     * {@link HillSlope#addConnectedDownstreamElementWithChech(HillSlope)}</b>
     * </p>
     * 
     * @param element the element that is tried to be added
     * @return
     */
    public boolean addConnectedUpstreamElementWithCheck( HillSlope element ) {
        if (PfafstetterNumber.areConnectedUpstream(this.getPfafstetterNumber(), element
                .getPfafstetterNumber())) {
            if (!upstreamElements.contains(element)) {
                upstreamElements.add(element);
                element.addConnectedDownstreamElementWithChech(this);
            }
            return true;
        }
        return false;
    }

    /**
     * Tries to add an element downstream to the actual one. A check is done on Pfafstetter to
     * understand if the passed element really is connected to the actual one. If it isn't the
     * element isn't added.
     * 
     * @param element the element that is tried to be added
     * @return
     */
    public boolean addConnectedDownstreamElementWithChech( HillSlope element ) {
        if (PfafstetterNumber.areConnectedDownstream(this.getPfafstetterNumber(), element
                .getPfafstetterNumber())) {
            downstreamElement = element;
            element.addConnectedUpstreamElementWithCheck(this);
            return true;
        }
        return false;
    }

    /**
     * @param pNum pfafstetter number object
     * @return the elementar basin that corrisponds to the supplied pfafstetter number
     */
    public HillSlope getUpstreamElementAtPfafstetter( PfafstetterNumber pNum ) {
        // am I the one
        if (pfafstetterNumber.compare(pfafstetterNumber, pNum) == 0) {
            return this;
        }
        // // perhaps my upstream elements
        // for( UpstreamElement upstreamElement : upstreamElements ) {
        // if (upstreamElement.getPfafstetterNumber().compare(
        // upstreamElement.getPfafstetterNumber(), pNum) == 0) {
        // return upstreamElement;
        // }
        // }
        // digg further
        HillSlope theChosen = null;
        for( HillSlope upstreamElement : upstreamElements ) {
            theChosen = upstreamElement.getUpstreamElementAtPfafstetter(pNum);
            if (theChosen != null) {
                break;
            }
        }
        return theChosen;
    }

    public HillSlope getConnectedDownstreamElement() {
        if (downstreamElement == null) {
            return null;
        }
        return downstreamElement;
    }

    /**
     * @return those upstream elements that are directly connected to the basin
     */
    public List<HillSlope> getConnectedUpstreamElements() {
        if (upstreamElements.size() > 0) {
            return upstreamElements;
        }
        return null;
    }

    /**
     * add all the upstream elements to a supplied list
     * 
     * @param elems
     */
    public void getAllUpstreamElements( List<HillSlope> elems, List<PfafstetterNumber> limit ) {
        // if the limit is the number of the actual element, return
        if (limit != null && limit.size() > 0) {
            for( PfafstetterNumber pfafs : limit ) {
                if (pfafs.compare(pfafs, pfafstetterNumber) == 0) {
                    return;
                }
            }
        }
        elems.add(this);
        for( HillSlope upstreamElement : upstreamElements ) {
            upstreamElement.getAllUpstreamElements(elems, limit);
        }
    }

    /**
     * add all the upstream element's geometries to a supplied list
     * 
     * @param elems
     * @param firstOfMaiorBasin
     */
    public void getAllUpstreamElementsGeometries( List<Geometry> elems,
            List<PfafstetterNumber> limit, HillSlope firstOfMaiorBasin ) {
        // if the limit is the number of the actual element, return
        if (limit != null && limit.size() > 0) {
            for( PfafstetterNumber pfafs : limit ) {
                if (pfafs.compare(pfafs, pfafstetterNumber) == 0) {
                    return;
                }
            }
        }
        this.firstOfMaiorBasin = firstOfMaiorBasin;
        elems.add((Geometry) hillslopeFeature.getDefaultGeometry());
        for( HillSlope upstreamElement : upstreamElements ) {
            upstreamElement.getAllUpstreamElementsGeometries(elems, limit, firstOfMaiorBasin);
        }

    }

    public int compare( HillSlope ue1, HillSlope ue2 ) {
        PfafstetterNumber p1 = ue1.getPfafstetterNumber();
        PfafstetterNumber p2 = ue2.getPfafstetterNumber();
        return p1.compare(p1, p2);
    }

    /**
     * Connect the various elements in a chain of tributary basins and nets
     * 
     * @param elements
     */
    public static void connectElements( List<HillSlope> elements ) {
        Collections.sort(elements, elements.get(0));

        for( int i = 0; i < elements.size(); i++ ) {
            HillSlope elem = elements.get(i);
            for( int j = i + 1; j < elements.size(); j++ ) {
                HillSlope tmp = elements.get(j);
                elem.addConnectedDownstreamElementWithChech(tmp);
            }
        }
    }

    @SuppressWarnings("nls")
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("=====================\n= PF: " + pfafstetterNumber).append("\n= ").append(
                "DownElem: \n= ");
        if (downstreamElement != null) {
            b.append(downstreamElement.pfafstetterNumber).append("\n= ");
        }
        b.append("UpElem:\n= ");
        for( int i = 0; i < upstreamElements.size(); i++ ) {
            if (upstreamElements.get(i) != null)
                b.append("\t" + upstreamElements.get(i).pfafstetterNumber).append("\n= ");
        }
        b.append("\n=====================\n");

        return b.toString();
    }

    public boolean hasVegetation() {
        return hasVegetation;
    }

    public final class Parameters {
        private final double depthMnSat;
        private final double ks;
        private final double mstExp;
        private final double recParam;
        private final double s2Param;
        private final double s2max;
        private final double eTrate;
        private final double s1residual;
        private final double s2residual;

        private HashMap<Integer, Double> laiMap;
        private HashMap<Integer, Double> displacementMap;
        private HashMap<Integer, Double> roughnessMap;
        private double RGL;
        private double rs;
        private double rarc;
        private double qsupmin;
        private double qsubmin;

        /**
         * Constructor for the {@link HillSlope}'s {@link Parameters}.
         */
        public Parameters() {
            depthMnSat = 2.5; // meters
            ks = 0.001; // 0.0008; //0.0025; //0.00066; [mphr]
            mstExp = 2.0; // dimensionless - default value 11.0

            double area_m2 = getHillslopeArea(); // [m^2]
            double spec_yield = 0.001; // dimensionless
            recParam = (700 * ks * depthMnSat) / (spec_yield * area_m2); // [1/hr]

            double porosity = 0.41; // 0.41; dimensionless
            // double d4_pm3 = 0.905 * (1. / (porosity * depthMnSat(hillSlope) * area_m2));
            s2max = porosity * depthMnSat * area_m2;
            s2Param = 0.905 * (1 / s2max); // [1/L^3]

            double etrate_mpd = 0.0004; // 0.0034;
            eTrate = etrate_mpd * (1. / 24.);

            s1residual = 0.02 * porosity * area_m2;

            s2residual = 0.007 * porosity * area_m2;

            qsupmin = 0.30 * 0.001;
            qsubmin = 0.70 * 0.001;
        }

        /**
         * Set the vegetation library parameters.
         * 
         * @param laiMap The {@link HashMap map} of vegetation id versus lai 
         *                  (leaf area index) HashMap.
         *                  <p>The key of the map is the vegetation id. The 
         *                  value of the map is the HashMap of lai defined 
         *                  differently for every month.</p>
         * @param displacementMap The {@link HashMap map} of the vegetation id 
         *                  versus the vegetation displacement height.
         *                  <p>The key of the map is the vegetation id. The 
         *                  value of the map is the HashMap of displacement defined 
         *                  differently for every month.</p>
         * @param roughnessMap The {@link HashMap map} of the vegetation id
         *                  versus the vegetation roughness.
         *                  <p>The key of the map is the vegetation id. The 
         *                  value of the map is the HashMap of roughness defined 
         *                  differently for every month.</p>
         * @param RGL the {@link HashMap} of the minimum incoming shortwave 
         *                  radiation at which there will be transpiration. The key
         *                  of the map is the vegetation id.
         * @param rs the {@link HashMap} of the minimum stomatal resistance 
         *                  function of vegetation type 0 for bare soil. The key
         *                  of the map is the vegetation id.
         * @param rarc the {@link HashMap} of the architectural resistance of 
         *                  the vegetation. The key of the map is the vegetation id.
         */
        public void setVegetationLibrary(
                HashMap<Integer, HashMap<Integer, Double>> vegindex2laiMap,
                HashMap<Integer, HashMap<Integer, Double>> vegindex2displacementMap,
                HashMap<Integer, HashMap<Integer, Double>> vegindex2roughnessMap,
                HashMap<Integer, Double> vegindex2RGLMap, HashMap<Integer, Double> vegindex2rsMap,
                HashMap<Integer, Double> vegindex2rarcMap ) {
            if (vegetationIdFieldIndex != -1) {
                int vegetationId = ((Number) hillslopeFeature.getAttribute(vegetationIdFieldIndex))
                        .intValue();
                if (vegetationId != -1) {
                    hasVegetation = true;
                    this.laiMap = vegindex2laiMap.get(vegetationId);
                    this.displacementMap = vegindex2displacementMap.get(vegetationId);
                    this.roughnessMap = vegindex2roughnessMap.get(vegetationId);
                    this.RGL = vegindex2RGLMap.get(vegetationId);
                    this.rs = vegindex2rsMap.get(vegetationId);
                    this.rarc = vegindex2rarcMap.get(vegetationId);
                }
            }
        }

        /**
         * Calculates the evapotraspiration.
         * 
         * @param month the current month index.
         * @param radiation net radiation from energy balance (W/m2).
         * @param pressure air pressure.
         * @param temperature air temperature.
         * @param shortRadiaton shortwave net radiation.
         * @param relativeHumidity air humidity.
         * @param windSpeed wind speed.
         * @param soilMoisture soil moisture.
         * @param snow water equivalent
         * @return evapotraspiration.
         */
        public double calculateEvapoTranspiration( int month, double radiation, double pressure,
                double temperature, double shortRadiaton, double relativeHumidity,
                double windSpeed, double soilMoisture, double snowWaterEquivalent ) {
            if (!hasVegetation()) {
                throw new ModelsIllegalargumentException(
                        "Evapotranspiration can be calculated only if the vegetation library has been defined. check your syntax...",
                        this.getClass().getSimpleName());
            }
            double evap = evapTransCalculator.penman(getBaricenterElevation(), radiation, rs, rarc,
                    laiMap.get(month), RGL, displacementMap.get(month), roughnessMap.get(month),
                    s2max, pressure, temperature, shortRadiaton, relativeHumidity, windSpeed,
                    soilMoisture, snowWaterEquivalent);
            return evap;
        }

        public double getDepthMnSat() {
            return depthMnSat;
        }

        public double getKs() {
            return ks;
        }

        public double getMstExp() {
            return mstExp;
        }

        public double getRecParam() {
            return recParam;
        }

        public double getS2Param() {
            return s2Param;
        }

        public double getS2max() {
            return s2max;
        }

        public double getETrate() {
            return eTrate;
        }

        public double getS1residual() {
            return s1residual;
        }

        public double getS2residual() {
            return s2residual;
        }

        public double getqqsupmin() {
            return qsupmin;
        }

        public double getqqsubmin() {
            return qsubmin;
        }

        public double getLai( int month ) {
            return laiMap.get(month);
        }

        public double getDisplacement( int month ) {
            return displacementMap.get(month);
        }

        public double getRoughness( int month ) {
            return roughnessMap.get(month);
        }

        public double getRGL() {
            return RGL;
        }

        public double getRs() {
            return rs;
        }

        public double getRarc() {
            return rarc;
        }

        // public double So() {
        // return 1.0; // So is max storage in the hillslope and i is the i-th link
        // }
        //
        // public double Ts() {
        // return 10.0;
        // }
        //
        // public double Te() {
        // return 1e20;
        // }

    }
}
