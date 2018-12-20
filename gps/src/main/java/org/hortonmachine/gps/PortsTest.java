package org.hortonmachine.gps;

import org.hortonmachine.gps.NmeaGps.NmeaGpsListener;
import org.hortonmachine.gps.utils.NmeaUtils;

import jssc.SerialPortException;
import net.sf.marineapi.provider.event.PositionEvent;
import net.sf.marineapi.provider.event.SatelliteInfoEvent;

public class PortsTest implements NmeaGpsListener {

    public PortsTest() throws SerialPortException {
        String[] availablePorts = NmeaGpsConnectionManager.INSTANCE.getAvailablePorts();
        System.out.println("List of available ports:");
        for( String port : availablePorts ) {
            System.out.println("--> " + port);
        }

        String findNmeaGpsPort = NmeaGpsConnectionManager.INSTANCE.findNmeaGpsPort();
        if (findNmeaGpsPort != null) {
            System.out.println("Found GPS at port: " + findNmeaGpsPort);

            NmeaGps gps = new NmeaGps(findNmeaGpsPort);
            gps.addListener(this);
            gps.start();

            int count = 0;
            while( count < 60 ) {
                count++;

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public static void main( String[] args ) throws SerialPortException {
        new PortsTest();
    }

    @Override
    public void onPositionEvent( PositionEvent positionEvent ) {
        String string = NmeaUtils.toString(positionEvent);
        System.out.println(string);
    }

    @Override
    public void onSatelliteInfoEvent( SatelliteInfoEvent satelliteInfoEvent ) {
        String string = NmeaUtils.toString(satelliteInfoEvent);
        System.out.println(string);
    }

}
