/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.network.pfafstetter;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the info of every channel of the network. Pfafstetter uses this class to compose the network.
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it
 * @author Andrea Antonello - www.hydrologis.com
 */
public class ChannelInfo {

    private boolean isTrim = false;

    private int hackOrder;

    private int channeParentNum;

    private String pfafParent;

    private List<Double> netNumComp = new ArrayList<Double>();

    private double pitValueFirst = 0;

    private double pitValueLast = 0;

    private List<String> pfafValue = new ArrayList<String>();

    private long origNetNumValue = 0;

    /**
     * hack order of the channel
     * 
     * @param hack
     */
    public void setHackOrder( int hack ) {
        this.hackOrder = hack;
    }

    /**
     * number of stream
     * 
     * @param netNum
     */
    public void addNetNumComp( double netNum ) {
        netNumComp.add(netNum);
    }

    /**
     * pit of first point of stream
     * 
     * @param netNum
     */
    public void addPitFirst( double pit ) {
        this.pitValueFirst = pit;
    }

    /**
     * pit of first point of stream
     * 
     * @param netNum
     */
    public void addPitLast( double pit ) {
        this.pitValueLast = pit;
    }

    /**
     * number of pfafstetter
     * 
     * @param pfaf
     */
    public void addPfafValue( String pfaf ) {
        pfafValue.add(0, pfaf);
    }

    /**
     * indicates if the channel has tributary channels
     * 
     * @param isTrim
     */
    public void setIsTrim( boolean isTrim ) {
        this.isTrim = isTrim;
    }

    /**
     * indicates the pfafstetter number of the parent channel
     * 
     * @param channeParentNum
     */
    public void setChannelParentNum( int channeParentNum ) {
        this.channeParentNum = channeParentNum;
    }

    /**
     * @param pfafParent
     */
    public void setPfafParent( String pfafParent ) {
        this.pfafParent = pfafParent;
    }

    /**
     * @param origNetNumValue
     */
    public void setOrigNetNumValue( long origNetNumValue ) {
        this.origNetNumValue = origNetNumValue;
    }

    /**
     * @return
     */
    public int getHackOrder() {
        return hackOrder;
    }

    /**
     * @return
     */
    public boolean getIsTrim() {
        return isTrim;
    }

    /**
     * @return
     */
    public List<Double> getNetNumComp() {
        return netNumComp;
    }

    /**
     * @return
     */
    public List<String> getPfafValue() {
        return pfafValue;
    }

    /**
     * @return
     */
    public int getChannelParentNum() {
        return channeParentNum;
    }

    /**
     * @return
     */
    public double getPitFirst() {
        return pitValueFirst;
    }

    /**
     * @return
     */
    public double getPitLast() {
        return pitValueLast;
    }

    /**
     * @return
     */
    public String getPfafParent() {
        return pfafParent;
    }

    /**
     * @return
     */
    public long getOrigNetNumValue() {
        return origNetNumValue;
    }
}
