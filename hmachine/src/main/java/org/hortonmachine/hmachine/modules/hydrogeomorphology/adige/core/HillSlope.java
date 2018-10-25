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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.network.PfafstetterNumber;
import org.hortonmachine.hmachine.modules.network.networkattributes.NetworkChannel;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * The hillslope area, related to a particular network link.
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class HillSlope implements IHillSlope {
    private int hillslopeId = -1;

    private SimpleFeature hillslopeFeature = null;
    private SimpleFeature linkFeature = null;
    private PfafstetterNumber pfafstetterNumber = null;

    private final List<IHillSlope> upstreamElements = new ArrayList<IHillSlope>();
    private IHillSlope downstreamElement = null;

    private IHillSlope firstOfMaiorBasin = null;
    private Geometry totalGeometryUpstream;
    private Random rn;
    private double hillslopeArea = -1;
    private double hillslopeUpstreamArea = -1;
    private double linkLength = -1;
    private double linkSlope = -1;

    private double baricenterElevation = -1;


    public HillSlope( SimpleFeature netFeature, SimpleFeature basinFeature, PfafstetterNumber pfafNumber, int hillslopeId ) {

        this.hillslopeId = hillslopeId;
        this.hillslopeFeature = basinFeature;
        this.linkFeature = netFeature;
        this.pfafstetterNumber = pfafNumber;

        rn = new Random();
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getHillslopeId()
     */
    public int getHillslopeId() {
        return hillslopeId;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getLinkFeature()
     */
    public SimpleFeature getLinkFeature() {
        return linkFeature;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getLinkLength()
     */
    public double getLinkLength() {
        if (linkLength == -1) {
            linkLength = ((Geometry) linkFeature.getDefaultGeometry()).getLength(); // [m]
        }
        return linkLength;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getLinkSlope()
     */
    public double getLinkSlope() {
        if ((int) linkSlope == -1) {
            // hillslopeFeature.getAttribute(baricenterElevationAttribute);
            double startElev = (Double) linkFeature.getAttribute(NetworkChannel.STARTELEVNAME);
            double endElev = (Double) linkFeature.getAttribute(NetworkChannel.ENDELEVNAME);
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

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getLinkWidth(double, double, double)
     */
    public double getLinkWidth( double coefficient, double exponent, double sdResiduals ) {
        // Returns a random value that follows a gaussian distribution
        double sampleGaussian = rn.nextGaussian() * sdResiduals;
        double upstreamArea = getUpstreamArea(null) / 1000000.0;

        double width = coefficient * Math.pow(upstreamArea, exponent) * Math.exp(sampleGaussian);
        return width;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getLinkChezi(double, double)
     */
    public double getLinkChezi( double coefficient, double exponent ) {
        double chezi = coefficient * Math.pow(getLinkSlope(), exponent);
        return chezi;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getHillslopeFeature()
     */
    public SimpleFeature getHillslopeFeature() {
        return hillslopeFeature;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getHillslopeArea()
     */
    public double getHillslopeArea() {
        if (hillslopeArea == -1) {
            hillslopeArea = ((Geometry) hillslopeFeature.getDefaultGeometry()).getArea(); // m^2
        }
        return hillslopeArea;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getBaricenterElevation()
     */
    public double getBaricenterElevation() {
        if (baricenterElevation == -1) {
            baricenterElevation = (Double) hillslopeFeature.getAttribute(NetworkChannel.BARICENTERELEVNAME);
        }
        return baricenterElevation;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getHillslopeClosure()
     */
    public Coordinate getHillslopeClosure() {
        Coordinate[] coords = ((Geometry) linkFeature.getDefaultGeometry()).getCoordinates();
        return coords[coords.length - 1];
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getGeometry(java.util.List, org.hortonmachine.gears.libs.monitor.IHMProgressMonitor, boolean)
     */
    public Geometry getGeometry( List<PfafstetterNumber> limit, IHMProgressMonitor pm, boolean doMonitor ) {

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
            pm.beginTask("Estrazione geometrie dei bacini elementari a monte", geometries.size() - 1);
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

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getUpstreamArea(java.util.List)
     */
    public double getUpstreamArea( List<PfafstetterNumber> limit ) {
        if (hillslopeUpstreamArea == -1) {

            List<IHillSlope> basins = new ArrayList<IHillSlope>();
            getAllUpstreamElements(basins, limit);

            hillslopeUpstreamArea = 0;
            for( IHillSlope elementarBasin : basins ) {
                hillslopeUpstreamArea = hillslopeUpstreamArea + elementarBasin.getHillslopeArea();
            }
        }
        return hillslopeUpstreamArea;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getPfafstetterNumber()
     */
    public PfafstetterNumber getPfafstetterNumber() {
        return pfafstetterNumber;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getFirstOfMaiorBasinElement()
     */
    public IHillSlope getFirstOfMaiorBasinElement() {
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

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#addConnectedUpstreamElementWithCheck(org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.HillSlope)
     */
    public boolean addConnectedUpstreamElementWithCheck( IHillSlope element ) {
        if (PfafstetterNumber.areConnectedUpstream(this.getPfafstetterNumber(), element.getPfafstetterNumber())) {
            if (!upstreamElements.contains(element)) {
                upstreamElements.add(element);
                element.addConnectedDownstreamElementWithCheck(this);
            }
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#addConnectedDownstreamElementWithChech(org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.HillSlope)
     */
    public boolean addConnectedDownstreamElementWithCheck( IHillSlope element ) {
        if (PfafstetterNumber.areConnectedDownstream(this.getPfafstetterNumber(), element.getPfafstetterNumber())) {
            downstreamElement = element;
            element.addConnectedUpstreamElementWithCheck(this);
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getUpstreamElementAtPfafstetter(org.hortonmachine.hmachine.modules.network.PfafstetterNumber)
     */
    public IHillSlope getUpstreamElementAtPfafstetter( PfafstetterNumber pNum ) {
        // am I the one
        if (pfafstetterNumber.compareTo(pNum) == 0) {
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
        IHillSlope theChosen = null;
        for( IHillSlope upstreamElement : upstreamElements ) {
            theChosen = upstreamElement.getUpstreamElementAtPfafstetter(pNum);
            if (theChosen != null) {
                break;
            }
        }
        return theChosen;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getConnectedDownstreamElement()
     */
    public IHillSlope getConnectedDownstreamElement() {
        if (downstreamElement == null) {
            return null;
        }
        return downstreamElement;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getConnectedUpstreamElements()
     */
    public List<IHillSlope> getConnectedUpstreamElements() {
        if (upstreamElements.size() > 0) {
            return upstreamElements;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getAllUpstreamElements(java.util.List, java.util.List)
     */
    public void getAllUpstreamElements( List<IHillSlope> elems, List<PfafstetterNumber> limit ) {
        // if the limit is the number of the actual element, return
        if (limit != null && limit.size() > 0) {
            for( PfafstetterNumber pfafs : limit ) {
                if (pfafs.compareTo(pfafstetterNumber) == 0) {
                    return;
                }
            }
        }
        elems.add(this);
        for( IHillSlope upstreamElement : upstreamElements ) {
            upstreamElement.getAllUpstreamElements(elems, limit);
        }
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#getAllUpstreamElementsGeometries(java.util.List, java.util.List, org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope)
     */
    public void getAllUpstreamElementsGeometries( List<Geometry> elems, List<PfafstetterNumber> limit,
            IHillSlope firstOfMaiorBasin ) {
        // if the limit is the number of the actual element, return
        if (limit != null && limit.size() > 0) {
            for( PfafstetterNumber pfafs : limit ) {
                if (pfafs.compareTo(pfafstetterNumber) == 0) {
                    return;
                }
            }
        }
        this.firstOfMaiorBasin = firstOfMaiorBasin;
        elems.add((Geometry) hillslopeFeature.getDefaultGeometry());
        for( IHillSlope upstreamElement : upstreamElements ) {
            upstreamElement.getAllUpstreamElementsGeometries(elems, limit, firstOfMaiorBasin);
        }

    }

    /**
     * Connect the various elements in a chain of tributary basins and nets
     * 
     * @param elements
     */
    public static void connectElements( List<IHillSlope> elements ) {
        Collections.sort(elements, elements.get(0));

        for( int i = 0; i < elements.size(); i++ ) {
            IHillSlope elem = elements.get(i);
            for( int j = i + 1; j < elements.size(); j++ ) {
                IHillSlope tmp = elements.get(j);
                elem.addConnectedDownstreamElementWithCheck(tmp);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#toString()
     */
    @SuppressWarnings("nls")
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("=====================\n= PF: " + pfafstetterNumber).append("\n= ").append("DownElem: \n= ");
        if (downstreamElement != null) {
            b.append(downstreamElement.getPfafstetterNumber()).append("\n= ");
        }
        b.append("UpElem:\n= ");
        for( int i = 0; i < upstreamElements.size(); i++ ) {
            if (upstreamElements.get(i) != null)
                b.append("\t" + upstreamElements.get(i).getPfafstetterNumber()).append("\n= ");
        }
        b.append("\n=====================\n");

        return b.toString();
    }

    /* (non-Javadoc)
     * @see org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.IHillSlope#compare(org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.HillSlope, org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core.HillSlope)
     */
    public int compare( IHillSlope ue1, IHillSlope ue2 ) {
        PfafstetterNumber p1 = ue1.getPfafstetterNumber();
        PfafstetterNumber p2 = ue2.getPfafstetterNumber();
        return p1.compareTo(p2);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + hillslopeId;
        result = prime * result + ((pfafstetterNumber == null) ? 0 : pfafstetterNumber.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj instanceof IHillSlope) {
            IHillSlope other = (IHillSlope) obj;
            PfafstetterNumber p1 = getPfafstetterNumber();
            PfafstetterNumber p2 = other.getPfafstetterNumber();
            return p1.compareTo(p2) == 0;
        }
        return false;
    }

}
