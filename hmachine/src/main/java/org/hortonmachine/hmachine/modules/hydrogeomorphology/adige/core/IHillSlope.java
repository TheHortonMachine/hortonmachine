package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core;

import java.util.Comparator;
import java.util.List;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.network.PfafstetterNumber;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public interface IHillSlope extends Comparator<IHillSlope> {

    public abstract int getHillslopeId();

    public abstract SimpleFeature getLinkFeature();

    /**
     * @return the length of the current hillslope's link. Dimension is meters.
     */
    public abstract double getLinkLength();

    /**
     * @return the slope of the current hillslope's link. The result is the tangent.
     */
    public abstract double getLinkSlope();

    /**
     * Assigns the channel widths of the links using a power law.
     * Width=coefficient*UpstreamArea[km2]^exponent+NORM(sdResiduals) where NORM() is a normally
     * distributed random variable.
     * 
     * @param coefficient The coefficient in the power law
     * @param exponent The exponent in the power law
     * @param sdResiduals The standard deviation of the residuals of the power law
     */
    public abstract double getLinkWidth( double coefficient, double exponent, double sdResiduals );

    /**
     * Assigns the Chezy coefficient of the links using a power law.
     * Chezi=coefficient*LinkSlope^exponent+NORM(sdResiduals) where NORM() is a normally distributed
     * random variable.
     * 
     * @param coefficient The coefficient in the power law
     * @param exponent The exponent in the power law
     */
    public abstract double getLinkChezi( double coefficient, double exponent );

    public abstract SimpleFeature getHillslopeFeature();

    /**
     * @return the area of the current hillslope. Dimension is meter^2
     */
    public abstract double getHillslopeArea();

    public abstract double getBaricenterElevation();

    /**
     * @return the closure coordinate of the basin, i.e. the last coordinate of the river
     */
    public abstract Coordinate getHillslopeClosure();

    /**
     * Get the geometry from the actual point to the passed numbers of pfafstetter
     * 
     * @param limit
     * @param pm
     * @param doMonitor
     * @return
     */
    public abstract Geometry getGeometry( List<PfafstetterNumber> limit, IHMProgressMonitor pm, boolean doMonitor );

    /**
     * Calculate the upstream area of the current hillslope.
     * 
     * @param limit a list of OmsPfafstetter numbers that define a list of hillslopes that block the
     *        recursion for area calculation. Through that for example we can define areas between
     *        two hillslopes.
     * @return the upstream area
     */
    public abstract double getUpstreamArea( List<PfafstetterNumber> limit );

    public abstract PfafstetterNumber getPfafstetterNumber();

    public abstract IHillSlope getFirstOfMaiorBasinElement();

    /**
     * Tries to add an element upstream to the actual one. A check is done on OmsPfafstetter to
     * understand if the passed element really is connected to the actual one. If it isn't the
     * element isn't added.
     * <p>
     * <b>Don't use this, this should usually be called only by:
     * {@link HillSlope#addConnectedDownstreamElementWithCheck(HillSlope)}</b>
     * </p>
     * 
     * @param element the element that is tried to be added
     * @return
     */
    public abstract boolean addConnectedUpstreamElementWithCheck( IHillSlope element );

    /**
     * Tries to add an element downstream to the actual one. A check is done on OmsPfafstetter to
     * understand if the passed element really is connected to the actual one. If it isn't the
     * element isn't added.
     * 
     * @param element the element that is tried to be added
     * @return
     */
    public abstract boolean addConnectedDownstreamElementWithCheck( IHillSlope element );

    /**
     * @param pNum pfafstetter number object
     * @return the elementar basin that corrisponds to the supplied pfafstetter number
     */
    public abstract IHillSlope getUpstreamElementAtPfafstetter( PfafstetterNumber pNum );

    public abstract IHillSlope getConnectedDownstreamElement();

    /**
     * @return those upstream elements that are directly connected to the basin
     */
    public abstract List<IHillSlope> getConnectedUpstreamElements();

    /**
     * add all the upstream elements to a supplied list
     * 
     * @param elems
     */
    public abstract void getAllUpstreamElements( List<IHillSlope> elems, List<PfafstetterNumber> limit );

    /**
     * add all the upstream element's geometries to a supplied list
     * 
     * @param elems
     * @param firstOfMaiorBasin
     */
    public abstract void getAllUpstreamElementsGeometries( List<Geometry> elems, List<PfafstetterNumber> limit,
            IHillSlope firstOfMaiorBasin );

    @SuppressWarnings("nls")
    public abstract String toString();

}