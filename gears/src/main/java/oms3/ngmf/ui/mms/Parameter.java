/*
 * $Id: Parameter.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.ngmf.ui.mms;

/**
 *
 * @author od
 */
public interface Parameter {

    public String getName();
    public int getWidth();
    public int getNumDim();
    public Dimension getDimension(int index);
    public int getSize();
    public Class getType();
    public Object getVals();
    public void setVals(Object vals);
    public void  setValueAt(Object val, int index);
    public boolean  isDimensionedBy(Dimension dim);
    public double getMean();
    public double getMin();
    public double getMax();
    public void resize();
}
