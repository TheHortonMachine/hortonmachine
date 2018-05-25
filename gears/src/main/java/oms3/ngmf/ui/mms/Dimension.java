/*
 * Dimension.java
 *
 * Created on June 23, 2005, 4:49 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package oms3.ngmf.ui.mms;

public interface Dimension {
    public String getName();
    public int getSize();
    public void setSize(int new_size);
    public String[] getItemNames();
    public String[] getItemDesc();
    public void addItemName(int i, String in, int size);
    public void addItemDesc(int i, String in, int size);
}
