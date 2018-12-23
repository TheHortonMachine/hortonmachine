package org.hortonmachine.gps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class TEst2 {
    int count = 0;
    int linecount = 0;
    volatile boolean isRunning = false;

    public TEst2() {
        SerialPort comPort = SerialPort.getCommPort("rfcomm0");
        try {
            StringBuilder sb = new StringBuilder();
            comPort.openPort();
            comPort.addDataListener(new SerialPortDataListener(){
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }
                @Override
                public void serialEvent( SerialPortEvent event ) {
                    if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                        System.out.println("EVENT TYPE: " + event.getEventType());
                        return;
                    }
                    byte[] newData = new byte[comPort.bytesAvailable()];
                    comPort.readBytes(newData, newData.length);
                    String x = new String(newData);
                    System.out.println(x);
//                    System.out.println(linecount++ + ") " + new String(newData));
                    sb.append(x);
                    if (count++ > 1000) {
                        comPort.closePort();
                        isRunning = false;

                        try (BufferedWriter bw = new BufferedWriter(
                                new FileWriter(new File("/home/hydrologis/Desktop/gps_log_events.nmea")))) {
                            bw.write(sb.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            isRunning = true;

            while( isRunning ) {
                Thread.sleep(100);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main( String[] args ) {
        new TEst2();
    }

}
