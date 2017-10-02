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
package org.hortonmachine.gears.io.geopaparazzi.geopap4;

/**
 * Represents an image.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Image  {
    /**
     * Image name.
     */
    private String name;
    /**
     * Image database id.
     */
    private final long id;

    /**
     * Image longitude.
     */
    private final double lon;
    /**
     * Image latitude.
     */
    private final double lat;
    /**
     * Image elevation.
     */
    private final double altim;
    /**
     * Image azimuth.
     */
    private final double azim;
    /**
     * Id of the image data.
     */
    private final long imageDataId;
    /**
     * Connected note id.
     */
    private long noteId;

    private final long ts;

    /**
     * A wrapper for an image.
     *
     * @param id          the image id.
     * @param name        the text of the note.
     * @param lon         lon
     * @param lat         lat
     * @param altim       elevation
     * @param azim        azimuth
     * @param imageDataId image data id.
     * @param noteId      note id.
     * @param ts          the timestamp.
     */
    public Image(long id, String name, double lon, double lat, double altim, double azim, long imageDataId, long noteId, long ts) {
        this.id = id;
        this.noteId = noteId;
        this.name = name;
        this.lon = lon;
        this.lat = lat;
        this.altim = altim;
        this.azim = azim;
        this.imageDataId = imageDataId;
        this.ts = ts;
    }

    public long getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    /**
     * @return the elevation.
     */
    public double getAltim() {
        return altim;
    }

    /**
     * @return the azimuth.
     */
    public double getAzim() {
        return azim;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the image data id.
     */
    public long getImageDataId() {
        return imageDataId;
    }

    public long getNoteId() {
        return noteId;
    }

    /**
     * @return the timestamp.
     */
    public long getTs() {
        return ts;
    }

    
    @Override
    public String toString() {
        return name;
    }

}
