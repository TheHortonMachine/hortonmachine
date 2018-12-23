package org.hortonmachine.gps.nmea;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

public interface INmeaGps extends SentenceListener {

    public void start() throws Exception;

    public void stop() throws Exception;

    public void addListener( NmeaGpsListener listener );

    public void removeListener( NmeaGpsListener listener );

    public default List<String> readFileToLinesList( File file ) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while( (line = br.readLine()) != null ) {
                lines.add(line);
            }
            return lines;
        }
    }

    public CurrentGpsInfo getCurrentGpsInfo();

    public default void sentenceRead( SentenceEvent event ) {
        Sentence sentence = event.getSentence();
        CurrentGpsInfo currentGpsInfo = getCurrentGpsInfo();
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

        List<NmeaGpsListener> listeners = getListeners();
        for( NmeaGpsListener nmeaGpsListener : listeners ) {
            nmeaGpsListener.onGpsEvent(currentGpsInfo);
        }

    }

    public List<NmeaGpsListener> getListeners();

}
