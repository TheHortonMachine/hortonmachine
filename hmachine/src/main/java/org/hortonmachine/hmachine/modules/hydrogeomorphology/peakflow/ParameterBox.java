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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow;

/**
 * @author Andrea Antonello - www.hydrologis.com
 */
public class ParameterBox {

    /*
     * parameters needed in the general and superficial case
     */
    private double n_idf = -9999.0f;
    private double a_idf = -9999.0f;
    private double area = -9999.0f;
    private double timestep = -9999.0f;
    private double diffusionParameterSup = -9999.0f;
    private double diffusionParameterSubSup = -9999.0f;
    private double vc = -9999.0f;
    private double delta = -9999.0f;
    private double xres = -9999.0f;
    private double yres = -9999.0f;
    private double npixel = -9999.0f;
    private double size = -9999.0f;
    private double[] time = null;
    private double[] pxl = null;

    /*
     * additional parameters needed in the subsuperficial case
     */
    private boolean isSubsuperficial = false;
    private double delta_sub = -9999.0f;
    private double npixel_sub = -9999.0f;
    private double area_sub = -9999.0f;
    private double v_sub = -9999.0f;
    private double resid_time = -9999.0f;
    private double[] time_sub = null;
    private double[] pxl_sub = null;

    /*
     * scs
     */
    private double vcvv = 0f;
    private double phi = 0.0;
    private int basinstate = -1;
    private boolean isScs = false;
    private String outputFile = null;

    /**
     * empty constructor. This is just a parameter box which holds some check on existend and
     * default values of certain parameters
     */
    public ParameterBox() {
    }

    public boolean isSubsuperficial() {
        return isSubsuperficial;
    }

    public void setSubsuperficial( boolean isSubsuperficial ) {
        this.isSubsuperficial = isSubsuperficial;
    }

    /*
     * SUPERFICIAL
     */
    public boolean a_idfExists() {
        if (a_idf != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getA_idf() {
        return a_idf;
    }

    public void setA_idf( double a_idf ) {
        this.a_idf = a_idf;
    }

    public boolean n_idfExists() {
        if (n_idf != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getN_idf() {
        return n_idf;
    }

    public void setN_idf( double n_idf ) {
        this.n_idf = n_idf;
    }

    public boolean areaExists() {
        if (area != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getArea() {
        return area;
    }

    public void setArea( double area ) {
        this.area = area;
    }

    public boolean area_subExists() {
        if (area_sub != -9999.0f) {
            return true;
        }
        return false;
    }

    public boolean timestepExists() {
        if (timestep != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getTimestep() {
        return timestep;
    }

    public void setTimestep( double timestep ) {
        this.timestep = timestep;
    }

    public boolean diffusionParameterSupExists() {
        return diffusionParameterSup != -9999.0f;
    }

    public double getDiffusionParameterSup() {
        return diffusionParameterSup;
    }

    public void setDiffusionParameterSup( double diffusionparameter ) {
        this.diffusionParameterSup = diffusionparameter;
    }

    public boolean diffusionParameterSubSupExists() {
    	return diffusionParameterSubSup != -9999.0f;
    }
    
    public double getDiffusionParameterSubSup() {
    	return diffusionParameterSubSup;
    }
    
    public void setDiffusionParameterSubSup( double diffusionparameter ) {
    	this.diffusionParameterSubSup = diffusionparameter;
    }

    public boolean vcExists() {
        if (vc != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getVc() {
        return vc;
    }

    public void setVc( double vc ) {
        this.vc = vc;
    }

    public boolean deltaExists() {
        if (delta != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getDelta() {
        return delta;
    }

    public void setDelta( double delta ) {
        this.delta = delta;
    }

    public boolean xresExists() {
        if (xres != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getXres() {
        return xres;
    }

    public void setXres( double xres ) {
        this.xres = xres;
    }

    public boolean yresExists() {
        if (yres != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getYres() {
        return yres;
    }

    public void setYres( double yres ) {
        this.yres = yres;
    }

    public boolean npixelExists() {
        if (npixel != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getNpixel() {
        return npixel;
    }

    public void setNpixel( double npixel ) {
        this.npixel = npixel;
    }

    public boolean sizeExists() {
        if (size != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getSize() {
        return size;
    }

    public void setSize( double size ) {
        this.size = size;
    }

    public boolean timeExists() {
        if (time != null) {
            return true;
        }
        return false;
    }

    public double[] getTime() {
        return time;
    }

    public void setTime( double[] time ) {
        this.time = time;
    }

    public boolean pxlExists() {
        if (pxl != null) {
            return true;
        }
        return false;
    }

    public double[] getPxl() {
        return pxl;
    }

    public void setPxl( double[] pxl ) {
        this.pxl = pxl;
    }

    /*
     * SUBSUPERFICIAL
     */
    public double getArea_sub() {
        return area_sub;
    }

    public void setArea_sub( double area_sub ) {
        this.area_sub = area_sub;
    }

    public boolean delta_subExists() {
        if (delta_sub != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getDelta_sub() {
        return delta_sub;
    }

    public void setDelta_sub( double delta_sub ) {
        this.delta_sub = delta_sub;
    }

    public boolean npixel_subExists() {
        if (npixel_sub != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getNpixel_sub() {
        return npixel_sub;
    }

    public void setNpixel_sub( double npixel_sub ) {
        this.npixel_sub = npixel_sub;
    }

    public boolean pxl_subExists() {
        if (pxl_sub != null) {
            return true;
        }
        return false;
    }

    public double[] getPxl_sub() {
        return pxl_sub;
    }

    public void setPxl_sub( double[] pxl_sub ) {
        this.pxl_sub = pxl_sub;
    }

    public boolean resid_timeExists() {
        if (resid_time != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getResid_time() {
        return resid_time;
    }

    public void setResid_time( double resid_time ) {
        this.resid_time = resid_time;
    }

    public boolean time_subExists() {
        if (time_sub != null) {
            return true;
        }
        return false;
    }

    public double[] getTime_sub() {
        return time_sub;
    }

    public void setTime_sub( double[] time_sub ) {
        this.time_sub = time_sub;
    }

    public boolean v_subExists() {
        if (v_sub != -9999.0f) {
            return true;
        }
        return false;
    }

    public double getV_sub() {
        return v_sub;
    }

    public void setV_sub( double v_sub ) {
        this.v_sub = v_sub;
    }

    public int getBasinstate() {
        return basinstate;
    }

    public void setBasinstate( int basinstate ) {
        this.basinstate = basinstate;
    }

    public double getVcvv() {
        return vcvv;
    }

    public void setVcvv( double vcvv ) {
        this.vcvv = vcvv;
    }

    public double getPhi() {
        return phi;
    }

    public void setPhi( double phi ) {
        this.phi = phi;
    }

    public boolean isScs() {
        return isScs;
    }

    public void setScs( boolean isScs ) {
        this.isScs = isScs;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile( String outputFile ) {
        this.outputFile = outputFile;
    }

}
