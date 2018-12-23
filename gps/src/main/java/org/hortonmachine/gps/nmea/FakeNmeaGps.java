package org.hortonmachine.gps.nmea;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gps.utils.CurrentGpsInfo;

import net.sf.marineapi.nmea.event.SentenceEvent;
import net.sf.marineapi.nmea.event.SentenceListener;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.sentence.GLLSentence;
import net.sf.marineapi.nmea.sentence.GSASentence;
import net.sf.marineapi.nmea.sentence.GSVSentence;
import net.sf.marineapi.nmea.sentence.RMCSentence;
import net.sf.marineapi.nmea.sentence.Sentence;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.SentenceValidator;

public class FakeNmeaGps implements INmeaGps {

    private static final int GPS_INTERVAL = 1000;
    private List<NmeaGpsListener> listeners = new ArrayList<>();
    private File dataFile;
    private List<String> dataLines;

    public boolean doCancel = false;

    private CurrentGpsInfo currentGpsInfo = new CurrentGpsInfo();

    public FakeNmeaGps( File dataFile ) {
        this.dataFile = dataFile;
    }

    public void start() throws Exception {
        dataLines = readFileToLinesList(dataFile);
        int linesCount = dataLines.size();

        new Thread(new Runnable(){
            public void run() {
                for( int i = 0; i < linesCount; i++ ) {
                    if (i == linesCount - 1) {
                        i = 0;
                    }
                    if (doCancel) {
                        break;
                    }

                    String line = dataLines.get(i);
                    if (SentenceValidator.isValid(line)) {
                        if (line.contains("MTK001")) {
                            continue;
                        }
                        ByteArrayInputStream source = new ByteArrayInputStream(line.getBytes());
                        SentenceReader reader = new SentenceReader(source);
                        reader.addSentenceListener(FakeNmeaGps.this, SentenceId.GSA);
                        reader.addSentenceListener(FakeNmeaGps.this, SentenceId.GLL);
                        reader.addSentenceListener(FakeNmeaGps.this, SentenceId.GSV);
                        reader.addSentenceListener(FakeNmeaGps.this, SentenceId.RMC);
                        reader.start();
                    }

                    try {
                        Thread.sleep(GPS_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    public void stop() throws Exception {
        doCancel = true;
    }

    public void addListener( NmeaGpsListener listener ) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    public void removeListener( NmeaGpsListener listener ) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    @Override
    public void readingPaused() {
        // TODO Auto-generated method stub

    }

    @Override
    public void readingStarted() {
        // TODO Auto-generated method stub

    }

    @Override
    public void readingStopped() {
        // TODO Auto-generated method stub

    }

    @Override
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

    public List<NmeaGpsListener> getListeners() {
        return listeners;
    }

    @Override
    public CurrentGpsInfo getCurrentGpsInfo() {
        return currentGpsInfo;
    }

    public static void main( String[] args ) throws Exception {
        FakeNmeaGps fg = new FakeNmeaGps(new File("/home/hydrologis/Desktop/gps_log_events.nmea"));
        fg.start();

        fg.addListener(new NmeaGpsListener(){

            @Override
            public void onGpsEvent( CurrentGpsInfo gpsInfo ) {
                System.out.println(gpsInfo);
            }
        });

    }
}
