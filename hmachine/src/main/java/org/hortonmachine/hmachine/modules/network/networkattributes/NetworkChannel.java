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
package org.hortonmachine.hmachine.modules.network.networkattributes;

import java.util.ArrayList;
import java.util.List;

import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * A netwrok class that permits net navigation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NetworkChannel {
    public static final String PFAFNAME = "pfaf";
    public static final String STRAHLERNAME = "strahler";
    public static final String HACKNAME = "hack";
    public static final String STARTELEVNAME = "startelev";
    public static final String ENDELEVNAME = "endelev";
    public static final String NETNUMNAME = "netnum";
    public static final String BARICENTERELEVNAME = "height"; // TODO

    private NetworkChannel nextChannel;
    private List<NetworkChannel> previousChannels = new ArrayList<NetworkChannel>();
    private SimpleFeature currentChannel;

    public NetworkChannel( SimpleFeature currentChannel ) {
        this.currentChannel = currentChannel;
    }

    public void setNext( NetworkChannel next ) {
        if (nextChannel != null && !nextChannel.equals(next)) {
            throw new RuntimeException();
        }
        nextChannel = next;
    }

    public NetworkChannel getNextChannel() {
        return nextChannel;
    }

    public List<NetworkChannel> getPreviousChannels() {
        return previousChannels;
    }

    public boolean isSource() {
        return previousChannels.size() == 0;
    }

    public void setStrahler( int value ) {
        currentChannel.setAttribute(STRAHLERNAME, value);
    }

    public void setPfafstetter( String value ) {
        currentChannel.setAttribute(PFAFNAME, value);
    }

    public int getStrahler() {
        Object attribute = currentChannel.getAttribute(STRAHLERNAME);
        if (attribute == null) {
            return -1;
        } else {
            return (Integer) attribute;
        }
    }

    public String getPfaf() {
        Object attribute = currentChannel.getAttribute(PFAFNAME);
        if (attribute == null) {
            return null;
        } else {
            return (String) attribute;
        }
    }

    public int getHack() {
        Object attribute = currentChannel.getAttribute(HACKNAME);
        if (attribute == null) {
            return -1;
        } else {
            return (Integer) attribute;
        }
    }

    public void addPrevious( NetworkChannel previous ) {
        if (!previousChannels.contains(previous)) {
            previousChannels.add(previous);
        }
    }

    public void checkAndAdd( NetworkChannel checkChannel ) {
        SimpleFeature checkChannelFeature = checkChannel.currentChannel;
        Geometry geometry = (Geometry) checkChannelFeature.getDefaultGeometry();
        Coordinate[] coordinates = geometry.getCoordinates();
        Coordinate first = coordinates[0];
        Coordinate last = coordinates[coordinates.length - 1];

        Geometry currentGeometry = (Geometry) currentChannel.getDefaultGeometry();
        Coordinate[] currentCoordinates = currentGeometry.getCoordinates();
        Coordinate currentFirst = currentCoordinates[0];
        Coordinate currentLast = currentCoordinates[currentCoordinates.length - 1];

        if (last.equals(currentFirst)) {
            addPrevious(checkChannel);
        }
        if (first.equals(currentLast)) {
            setNext(checkChannel);
        }
    }

}
