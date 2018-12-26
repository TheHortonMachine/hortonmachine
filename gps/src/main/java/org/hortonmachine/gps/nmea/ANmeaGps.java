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

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gps.utils.CurrentGpsInfo;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.sentence.GLLSentence;
import net.sf.marineapi.nmea.sentence.GSASentence;
import net.sf.marineapi.nmea.sentence.GSVSentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.sentence.Sentence;

/**
 * Abstract NMEA GPS object
 */
public abstract class ANmeaGps implements SentenceListener {
    protected List<NmeaGpsListener> listeners = new ArrayList<>();
    protected boolean doCancel = false;
    protected CurrentGpsInfo currentGpsInfo = new CurrentGpsInfo();

    /**
     * Start the connection tpo the GPS.
     * 
     * @throws Exception
     */
    public abstract void start() throws Exception;

    /**
     * Stop the connection to the GPS.
     * 
     * @throws Exception
     */
    public void stop() throws Exception {
        doCancel = true;
        listeners.clear();
    }

    /**
     * Add a {@link NmeaGpsListener}.
     * 
     * @param listener the listener to add.
     */
    public void addListener( NmeaGpsListener listener ) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a {@link NmeaGpsListener}.
     * 
     * @param listener the listener to remove.
     */
    public void removeListener( NmeaGpsListener listener ) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    public void sentenceRead( SentenceEvent event ) {
        Sentence sentence = event.getSentence();
        if (sentence instanceof GSASentence) {
            GSASentence gsa = (GSASentence) sentence;
            currentGpsInfo.addGSA(gsa);
        } else if (sentence instanceof GLLSentence) {
            GLLSentence gll = (GLLSentence) sentence;
            currentGpsInfo.addGLL(gll);
        } else if (sentence instanceof GSVSentence) {
            GSVSentence gsv = (GSVSentence) sentence;
            currentGpsInfo.addGSV(gsv);
        } else if (sentence instanceof RMCSentence) {
            RMCSentence rmc = (RMCSentence) sentence;
            currentGpsInfo.addRMC(rmc);
        }

        for( NmeaGpsListener nmeaGpsListener : listeners ) {
            nmeaGpsListener.onGpsEvent(currentGpsInfo);
        }

    }

    public void readingPaused() {
    }

    public void readingStarted() {
    }

    public void readingStopped() {
    }

}
