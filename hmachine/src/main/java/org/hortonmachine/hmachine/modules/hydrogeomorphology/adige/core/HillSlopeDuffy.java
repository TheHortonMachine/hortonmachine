/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.core;

import java.util.List;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.duffy.DuffyInputs;
import org.hortonmachine.hmachine.modules.network.PfafstetterNumber;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * A {@link HillSlope} tweaked for the Duffy model. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public class HillSlopeDuffy implements IHillSlope {

    private Parameters parameters;
    private final IHillSlope hillSlope;

    public HillSlopeDuffy( IHillSlope hillSlope, DuffyInputs duffyInputs ) {
        this.hillSlope = hillSlope;
        parameters = new Parameters(duffyInputs.pKs, duffyInputs.pMstexp, duffyInputs.pSpecyield, duffyInputs.pPorosity,
                duffyInputs.pEtrate, duffyInputs.pSatconst, duffyInputs.pDepthmnsat);
    }

    public Parameters getParameters() {
        return parameters;
    }

    public final class Parameters {
        private final double recParam;
        private final double s2Param;
        private final double s2max;
        private final double s1residual;
        private final double s2residual;

        private double qsupmin;
        private double qsubmin;
        private final double pDepthmnsat;
        private final double pKs;
        private final double pMstexp;
        private Double pEtrate;

        /**
         * Constructor for the {@link HillSlope}'s {@link Parameters}.
         * 
         * @param pSatconst 
         * @param pEtrate 
         * @param pPorosity 
         * @param pSpecyield 
         * @param pMstexp 
         * @param pKs 
         * @param pDepthmnsat 
         */
        public Parameters( double pKs, double pMstexp, double pSpecyield, double pPorosity, Double pEtrate, double pSatconst,
                double pDepthmnsat ) {

            this.pKs = pKs;
            this.pMstexp = pMstexp;
            this.pDepthmnsat = pDepthmnsat;
            if (pEtrate != null) {
                this.pEtrate = pEtrate * (1. / 24.);
            }

            double area_m2 = getHillslopeArea(); // [m^2]
            recParam = (pSatconst * pKs * pDepthmnsat) / (pSpecyield * area_m2); // [1/hr]

            // double d4_pm3 = 0.905 * (1. / (porosity * depthMnSat(hillSlope) * area_m2));
            s2max = pPorosity * pDepthmnsat * area_m2;
            s2Param = 0.905 * (1 / s2max); // [1/L^3]

            s1residual = 0.02 * pPorosity * area_m2;

            s2residual = 0.007 * pPorosity * area_m2;

            qsupmin = 0.30 * 0.001;
            qsubmin = 0.70 * 0.001;
        }

        public double getDepthMnSat() {
            return pDepthmnsat;
        }

        public double getKs() {
            return pKs;
        }

        public double getMstExp() {
            return pMstexp;
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

        public Double getETrate() {
            return pEtrate;
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

    public int getHillslopeId() {
        return hillSlope.getHillslopeId();
    }

    public SimpleFeature getLinkFeature() {
        return hillSlope.getLinkFeature();
    }

    public double getLinkLength() {
        return hillSlope.getLinkLength();
    }

    public double getLinkSlope() {
        return hillSlope.getLinkSlope();
    }

    public double getLinkWidth( double coefficient, double exponent, double sdResiduals ) {
        return hillSlope.getLinkWidth(coefficient, exponent, sdResiduals);
    }

    public double getLinkChezi( double coefficient, double exponent ) {
        return hillSlope.getLinkChezi(coefficient, exponent);
    }

    public SimpleFeature getHillslopeFeature() {
        return hillSlope.getHillslopeFeature();
    }

    public double getHillslopeArea() {
        return hillSlope.getHillslopeArea();
    }

    public double getBaricenterElevation() {
        return hillSlope.getBaricenterElevation();
    }

    public Coordinate getHillslopeClosure() {
        return hillSlope.getHillslopeClosure();
    }

    public Geometry getGeometry( List<PfafstetterNumber> limit, IHMProgressMonitor pm, boolean doMonitor ) {
        return hillSlope.getGeometry(limit, pm, doMonitor);
    }

    public double getUpstreamArea( List<PfafstetterNumber> limit ) {
        return hillSlope.getUpstreamArea(limit);
    }

    public PfafstetterNumber getPfafstetterNumber() {
        return hillSlope.getPfafstetterNumber();
    }

    public IHillSlope getFirstOfMaiorBasinElement() {
        return hillSlope.getFirstOfMaiorBasinElement();
    }

    public boolean addConnectedUpstreamElementWithCheck( IHillSlope element ) {
        return hillSlope.addConnectedUpstreamElementWithCheck(element);
    }

    public boolean addConnectedDownstreamElementWithCheck( IHillSlope element ) {
        return hillSlope.addConnectedDownstreamElementWithCheck(element);
    }

    public IHillSlope getUpstreamElementAtPfafstetter( PfafstetterNumber pNum ) {
        return hillSlope.getUpstreamElementAtPfafstetter(pNum);
    }

    public IHillSlope getConnectedDownstreamElement() {
        return hillSlope.getConnectedDownstreamElement();
    }

    public List<IHillSlope> getConnectedUpstreamElements() {
        return hillSlope.getConnectedUpstreamElements();
    }

    public void getAllUpstreamElements( List<IHillSlope> elems, List<PfafstetterNumber> limit ) {
        hillSlope.getAllUpstreamElements(elems, limit);
    }

    public void getAllUpstreamElementsGeometries( List<Geometry> elems, List<PfafstetterNumber> limit,
            IHillSlope firstOfMaiorBasin ) {
        hillSlope.getAllUpstreamElementsGeometries(elems, limit, firstOfMaiorBasin);
    }

    public int compare( IHillSlope ue1, IHillSlope ue2 ) {
        PfafstetterNumber p1 = ue1.getPfafstetterNumber();
        PfafstetterNumber p2 = ue2.getPfafstetterNumber();
        return p1.compareTo(p2);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getHillslopeId();
        result = prime * result + ((getPfafstetterNumber() == null) ? 0 : getPfafstetterNumber().hashCode());
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
