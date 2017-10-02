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
package org.hortonmachine.gears.utils.coverage;

import javax.media.jai.iterator.RandomIter;

public class ConstantRandomIter implements RandomIter {

    double doubleValue = 0;

    float floatValue = 0;

    int intValue = 0;
    
    boolean isDouble=false;
    boolean isInt=false;
    boolean isFloat=false;


    public ConstantRandomIter(double value) {
        this.doubleValue=value;
       isDouble=true;

        
        
        
    }

    public ConstantRandomIter(float value) {
        this.floatValue=value;
        isFloat=true;
    }

    public ConstantRandomIter(int value) {
        this.intValue=value;
        isInt=true;

    }

    public void done() {
        // TODO Auto-generated method stub

    }

    public int[] getPixel(int x, int y, int[] iArray) {
        // TODO Auto-generated method stub
        return null;
    }

    public float[] getPixel(int x, int y, float[] fArray) {
        // TODO Auto-generated method stub
        return null;
    }

    public double[] getPixel(int x, int y, double[] dArray) {
        // TODO Auto-generated method stub
        return null;
    }

    public int getSample(int x, int y, int b) {
        if(isInt){
            return this.intValue;
        }else if(isDouble){
            return (int) this.doubleValue;
        } if(isFloat){
            return (int) this.floatValue;
        }
        return intValue;
        
     
    }

    public double getSampleDouble(int x, int y, int b) {
        if(isInt){
            return (double) this.intValue;
        }else if(isDouble){
            return  this.doubleValue;
        } if(isFloat){
            return (double) this.floatValue;
        }
        return doubleValue;
    }

    public float getSampleFloat(int x, int y, int b) {
        if(isInt){
            return  (float) this.intValue;
        }else if(isDouble){
            return (float) this.doubleValue;
        } if(isFloat){
            return  this.floatValue;
        }
        return floatValue;
    }

}
