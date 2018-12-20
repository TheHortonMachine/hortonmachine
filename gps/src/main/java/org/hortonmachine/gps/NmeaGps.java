package org.hortonmachine.gps;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.parser.SentenceParser;
import net.sf.marineapi.nmea.sentence.SentenceValidator;
import net.sf.marineapi.provider.PositionProvider;
import net.sf.marineapi.provider.SatelliteInfoProvider;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.SatelliteInfoEvent;

public class NmeaGps implements SerialPortEventListener {

    private SerialPort serialPort;

    public interface NmeaGpsListener {
        public void onPositionEvent( PositionEvent positionEvent );
        public void onSatelliteInfoEvent( SatelliteInfoEvent satelliteInfoEvent );
    }

    private List<NmeaGpsListener> listeners = new ArrayList<>();
    private String port;

    public NmeaGps( String port ) {
        this.port = port;
    }

    public void start() throws SerialPortException {
        serialPort = NmeaGpsConnectionManager.INSTANCE.getConnection(port);
        if (serialPort != null) {
            serialPort.addEventListener(this);
        } else {
            throw new RuntimeException();
        }
    }

    public void stop() throws SerialPortException {
        if (serialPort != null && serialPort.isOpened()) {
            serialPort.closePort();
        }
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
    public void serialEvent( SerialPortEvent event ) {
        if (event.isRXCHAR() && event.getEventValue() > 0) {
            try {
                String receivedData = serialPort.readString(event.getEventValue());
                if (receivedData == null) {
                    return;
                }
                String[] split = receivedData.split("\n");
                for( String line : split ) {
                    boolean isSentence = SentenceValidator.isSentence(line);
                    if (isSentence) {
                        InputStream targetStream = new ByteArrayInputStream(line.getBytes());

                        SentenceReader reader = new SentenceReader(targetStream);
                        SatelliteInfoProvider satProvider = new SatelliteInfoProvider(reader);
                        satProvider.addListener(evt -> onSatelliteInfoEvent(evt));
                        PositionProvider positionProvider = new PositionProvider(reader);
                        positionProvider.addListener(evt -> onPositionEvent(evt));

                        reader.start();
                    }
                }

//                boolean isSentence = SentenceValidator.isSentence(receivedData);
//                boolean isValid = SentenceValidator.isValid(receivedData);

            } catch (SerialPortException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void onPositionEvent( PositionEvent pEvent ) {
        for( NmeaGpsListener nmeaGpsListener : listeners ) {
            nmeaGpsListener.onPositionEvent(pEvent);
        }
    }

    private void onSatelliteInfoEvent( SatelliteInfoEvent evt ) {
        for( NmeaGpsListener nmeaGpsListener : listeners ) {
            nmeaGpsListener.onSatelliteInfoEvent(evt);
        }
    }

}
