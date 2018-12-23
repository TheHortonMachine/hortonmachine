package org.hortonmachine.gps;

import com.fazecast.jSerialComm.SerialPort;

public class TEst {

    public TEst() {
        SerialPort[] commPorts = SerialPort.getCommPorts();
        for( SerialPort comPort : commPorts ) {
            System.out.println("Port: " + comPort.getSystemPortName());
            try {
                comPort.openPort();
                int count = 0;
                int emptyCount = 0;
                int linecount = 0;
                while( count++ < 1000 ) {
                    while( comPort.bytesAvailable() < 1 && emptyCount < 100 ) {
                        Thread.sleep(20);
                        emptyCount++;
                    }

                    byte[] readBuffer = new byte[comPort.bytesAvailable()];

                    comPort.readBytes(readBuffer, readBuffer.length);
                    System.out.println(linecount++ + ") " + new String(readBuffer));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                comPort.closePort();
            }
            System.out.println("-----------------------------------");
        }

    }

    public static void main( String[] args ) {
        new TEst();
    }

}
