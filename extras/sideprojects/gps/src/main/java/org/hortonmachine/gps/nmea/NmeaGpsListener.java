/*******************************************************************************
 * Copyright (C) 2018 HydroloGIS S.r.l. (www.hydrologis.com)
 * 
 * This program is free software: you can redistribute it and/or modify
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
 * 
 * Author: Antonello Andrea (http://www.hydrologis.com)
 ******************************************************************************/
package org.hortonmachine.gps.nmea;

import org.hortonmachine.gps.utils.CurrentGpsInfo;

/**
 * Interface for NMEA GPS listeners.
 */
public interface NmeaGpsListener {
    /**
     * Triggered on each valid GPS data event.
     * 
     * @param gpsInfo the gps information at its current updated state.
     */
    public void onGpsEvent( CurrentGpsInfo gpsInfo );
}
