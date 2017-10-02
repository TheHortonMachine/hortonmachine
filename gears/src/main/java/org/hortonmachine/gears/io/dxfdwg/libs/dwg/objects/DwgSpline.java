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
package org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects;

import java.util.Vector;

import org.hortonmachine.gears.io.dxfdwg.libs.dwg.DwgObject;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.DwgUtil;

/**
 * The DwgSpline class represents a DWG Spline
 * 
 * @author jmorell
 */
public class DwgSpline extends DwgObject {
	private int scenario;
	private int degree;
	private double fitTolerance;
	private double[] beginTanVector;
	private double[] endTanVector;
	private boolean rational;
	private boolean closed;
	private boolean periodic;
	private double knotTolerance;
	private double controlTolerance;
	private double[] knotPoints;
	private double[][] controlPoints;
	private double[] weights;
	private double[][] fitPoints;
	
	/**
	 * Read a Spline in the DWG format Version 15
	 * 
	 * @param data Array of unsigned bytes obtained from the DWG binary file
	 * @param offset The current bit offset where the value begins
	 * @throws Exception If an unexpected bit value is found in the DWG file. Occurs
	 * 		   when we are looking for LwPolylines.
	 */
	public void readDwgSplineV15(int[] data, int offset) throws Exception {
		int bitPos = offset;
		bitPos = readObjectHeaderV15(data, bitPos);
		Vector v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int sc = ((Integer)v.get(1)).intValue();
		scenario = sc;
		v = DwgUtil.getBitShort(data, bitPos);
		bitPos = ((Integer)v.get(0)).intValue();
		int deg = ((Integer)v.get(1)).intValue();
		degree = deg;
		int knotsNumber = 0;
		int controlPointsNumber = 0;
		int fitPointsNumber = 0;
		boolean weight = false;
		if (sc==2) {
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double ft = ((Double)v.get(1)).doubleValue();
			fitTolerance = ft;
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double x = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double y = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double z = ((Double)v.get(1)).doubleValue();
			double[] coord = new double[]{x, y, z};
			beginTanVector = coord;
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			x = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			y = ((Double)v.get(1)).doubleValue();
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			z = ((Double)v.get(1)).doubleValue();
			coord = new double[]{x, y, z};
			endTanVector = coord;
			v = DwgUtil.getBitShort(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			fitPointsNumber = ((Integer)v.get(1)).intValue();
		} else if (sc==1) {
			v = DwgUtil.testBit(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			boolean rat = ((Boolean)v.get(1)).booleanValue();
			rational = rat;
			v = DwgUtil.testBit(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			boolean closed = ((Boolean)v.get(1)).booleanValue();
			this.closed = closed;
			v = DwgUtil.testBit(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			boolean per = ((Boolean)v.get(1)).booleanValue();
			periodic = per;
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double ktol = ((Double)v.get(1)).doubleValue();
			knotTolerance = ktol;
			v = DwgUtil.getBitDouble(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			double ctol = ((Double)v.get(1)).doubleValue();
			controlTolerance = ctol;
			v = DwgUtil.getBitLong(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			knotsNumber = ((Integer)v.get(1)).intValue();
			v = DwgUtil.getBitLong(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			controlPointsNumber = ((Integer)v.get(1)).intValue();
			v = DwgUtil.testBit(data, bitPos);
			bitPos = ((Integer)v.get(0)).intValue();
			weight = ((Boolean)v.get(1)).booleanValue();
		} else {
			System.out.println("ERROR: Escenario desconocido");
		}
		if (knotsNumber>0) {
			double[] knotpts = new double[knotsNumber];
			for (int i=0;i<knotsNumber;i++) {
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				knotpts[i] = ((Double)v.get(1)).doubleValue();
			}
			knotPoints = knotpts;
		}
		if (controlPointsNumber>0) {
			// Si el n?mero de weights no coincide con el de ctrlpts habr? problemas ...
			double[][] ctrlpts = new double[controlPointsNumber][3];
			double[] weights = new double[controlPointsNumber];
			for (int i=0;i<controlPointsNumber;i++) {
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				double x = ((Double)v.get(1)).doubleValue();
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				double y = ((Double)v.get(1)).doubleValue();
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				double z = ((Double)v.get(1)).doubleValue();
				//double[] coord = new double[]{x, y, z};
				ctrlpts[i][0] = x;
				ctrlpts[i][1] = y;
				ctrlpts[i][2] = z;
				if (weight) {
					v = DwgUtil.getBitDouble(data, bitPos);
					bitPos = ((Integer)v.get(0)).intValue();
					weights[i] = ((Double)v.get(1)).doubleValue();
				}
			}
			controlPoints = ctrlpts;
			if (weight) {
				this.weights = weights;
			}
		}
		if (fitPointsNumber>0) {
			double[][] fitpts = new double[fitPointsNumber][3];
			for (int i=0;i<fitPointsNumber;i++) {
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				double x = ((Double)v.get(1)).doubleValue();
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				double y = ((Double)v.get(1)).doubleValue();
				v = DwgUtil.getBitDouble(data, bitPos);
				bitPos = ((Integer)v.get(0)).intValue();
				double z = ((Double)v.get(1)).doubleValue();
				fitpts[i][0] = x;
				fitpts[i][1] = y;
				fitpts[i][2] = z;
			}
			fitPoints = fitpts;
		}
		bitPos = readObjectTailV15(data, bitPos);
	}
	/**
	 * @return Returns the closed.
	 */
	public boolean isClosed() {
		return closed;
	}
	/**
	 * @param closed The closed to set.
	 */
	public void setClosed(boolean closed) {
		this.closed = closed;
	}
	/**
	 * @return Returns the controlPoints.
	 */
	public double[][] getControlPoints() {
		return controlPoints;
	}
	/**
	 * @param controlPoints The controlPoints to set.
	 */
	public void setControlPoints(double[][] controlPoints) {
		this.controlPoints = controlPoints;
	}
	/**
	 * @return Returns the fitPoints.
	 */
	public double[][] getFitPoints() {
		return fitPoints;
	}
	/**
	 * @param fitPoints The fitPoints to set.
	 */
	public void setFitPoints(double[][] fitPoints) {
		this.fitPoints = fitPoints;
	}
	/**
	 * @return Returns the knotPoints.
	 */
	public double[] getKnotPoints() {
		return knotPoints;
	}
	/**
	 * @param knotPoints The knotPoints to set.
	 */
	public void setKnotPoints(double[] knotPoints) {
		this.knotPoints = knotPoints;
	}
	/**
	 * @return Returns the scenario.
	 */
	public int getScenario() {
		return scenario;
	}
	/**
	 * @param scenario The scenario to set.
	 */
	public void setScenario(int scenario) {
		this.scenario = scenario;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public Object clone() {
		DwgSpline dwgSpline = new DwgSpline();
		dwgSpline.setType(type);
		dwgSpline.setHandle(handle);
		dwgSpline.setVersion(version);
		dwgSpline.setMode(mode);
		dwgSpline.setLayerHandle(layerHandle);
		dwgSpline.setColor(color);
		dwgSpline.setNumReactors(numReactors);
		dwgSpline.setNoLinks(noLinks);
		dwgSpline.setLinetypeFlags(linetypeFlags);
		dwgSpline.setPlotstyleFlags(plotstyleFlags);
		dwgSpline.setSizeInBits(sizeInBits);
		dwgSpline.setExtendedData(extendedData);
		dwgSpline.setGraphicData(graphicData);
		//dwgSpline.setInsideBlock(insideBlock);
		dwgSpline.setScenario(scenario);
		dwgSpline.setDegree(degree);
		dwgSpline.setFitTolerance(fitTolerance);
		dwgSpline.setBeginTanVector(beginTanVector);
		dwgSpline.setEndTanVector(endTanVector);
		dwgSpline.setRational(rational);
		dwgSpline.setClosed(closed);
		dwgSpline.setPeriodic(periodic);
		dwgSpline.setKnotTolerance(knotTolerance);
		dwgSpline.setControlTolerance(controlTolerance);
		dwgSpline.setKnotPoints(knotPoints);
		dwgSpline.setControlPoints(controlPoints);
		dwgSpline.setWeights(weights);
		dwgSpline.setFitPoints(fitPoints);
		return dwgSpline;
	}
	/**
	 * @return Returns the beginTanVector.
	 */
	public double[] getBeginTanVector() {
		return beginTanVector;
	}
	/**
	 * @param beginTanVector The beginTanVector to set.
	 */
	public void setBeginTanVector(double[] beginTanVector) {
		this.beginTanVector = beginTanVector;
	}
	/**
	 * @return Returns the controlTolerance.
	 */
	public double getControlTolerance() {
		return controlTolerance;
	}
	/**
	 * @param controlTolerance The controlTolerance to set.
	 */
	public void setControlTolerance(double controlTolerance) {
		this.controlTolerance = controlTolerance;
	}
	/**
	 * @return Returns the degree.
	 */
	public int getDegree() {
		return degree;
	}
	/**
	 * @param degree The degree to set.
	 */
	public void setDegree(int degree) {
		this.degree = degree;
	}
	/**
	 * @return Returns the endTanVector.
	 */
	public double[] getEndTanVector() {
		return endTanVector;
	}
	/**
	 * @param endTanVector The endTanVector to set.
	 */
	public void setEndTanVector(double[] endTanVector) {
		this.endTanVector = endTanVector;
	}
	/**
	 * @return Returns the fitTolerance.
	 */
	public double getFitTolerance() {
		return fitTolerance;
	}
	/**
	 * @param fitTolerance The fitTolerance to set.
	 */
	public void setFitTolerance(double fitTolerance) {
		this.fitTolerance = fitTolerance;
	}
	/**
	 * @return Returns the knotTolerance.
	 */
	public double getKnotTolerance() {
		return knotTolerance;
	}
	/**
	 * @param knotTolerance The knotTolerance to set.
	 */
	public void setKnotTolerance(double knotTolerance) {
		this.knotTolerance = knotTolerance;
	}
	/**
	 * @return Returns the periodic.
	 */
	public boolean isPeriodic() {
		return periodic;
	}
	/**
	 * @param periodic The periodic to set.
	 */
	public void setPeriodic(boolean periodic) {
		this.periodic = periodic;
	}
	/**
	 * @return Returns the rational.
	 */
	public boolean isRational() {
		return rational;
	}
	/**
	 * @param rational The rational to set.
	 */
	public void setRational(boolean rational) {
		this.rational = rational;
	}
	/**
	 * @return Returns the weights.
	 */
	public double[] getWeights() {
		return weights;
	}
	/**
	 * @param weights The weights to set.
	 */
	public void setWeights(double[] weights) {
		this.weights = weights;
	}
}
