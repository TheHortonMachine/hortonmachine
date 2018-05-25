/*
 * Parameter.java
 *
 * Created on June 23, 2005, 4:50 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package oms3.ngmf.ui.mms;

/**
 *
 * @author Olaf David
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
