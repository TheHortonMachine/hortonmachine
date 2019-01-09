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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import net.sf.marineapi.nmea.io.SentenceReader;
import net.sf.marineapi.nmea.sentence.SentenceId;
import net.sf.marineapi.nmea.sentence.SentenceValidator;

/**
 * A NMEA GPS object that reads from serial port NMEA strings.
 */
public class SerialNmeaGps extends ANmeaGps implements SerialPortDataListener {

    private static final String HM_START = "$HM:";
    private String comPort;
    private SerialPort commPort;

    private BufferedWriter logWriter;
    private int baudRate = BAUDRATE_115200;
    private int numDataBits = DATABITS_8;
    private int numStopBits = STOPBITS_1;
    private int parity = PARITY_NONE;

    private boolean isHmDevice = false;
    private boolean deviceChecked = false;
    private String hmDataString = "";

    public SerialNmeaGps( String comPort ) {
        this.comPort = comPort;
        isHmDevice = false;
        deviceChecked = false;
    }

    public SerialNmeaGps( String comPort, int baudRate, int numDataBits, int numStopBits, int parity ) {
        this.comPort = comPort;
        this.baudRate = baudRate;
        this.numDataBits = numDataBits;
        this.numStopBits = numStopBits;
        this.parity = parity;
        isHmDevice = false;
        deviceChecked = false;
    }

    public void logToFile( File logFile ) throws IOException {
        if (logFile != null)
            logWriter = new BufferedWriter(new FileWriter(logFile));
    }

    @Override
    public void start() throws Exception {
        commPort = SerialPort.getCommPort(comPort);
        commPort.setComPortParameters(baudRate, numDataBits, numStopBits, parity);
        boolean opened = commPort.openPort();
        if (opened) {
            commPort.addDataListener(this);
        } else {
            throw new RuntimeException("Unable to connect to port: " + comPort);
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        commPort.closePort();
        if (logWriter != null) {
            logWriter.close();
        }
    }

    /**
     * Getter for the available serial ports.
     * 
     * @return the available serial ports system names.
     */
    public static String[] getAvailablePortNames() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] portNames = new String[ports.length];
        for( int i = 0; i < portNames.length; i++ ) {
            String systemPortName = ports[i].getSystemPortName();
            portNames[i] = systemPortName;
        }
        return portNames;
    }

    public static SerialPort[] getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        return ports;
    }

    /**
     * Makes a blocking check to find out if the given port supplies GPS data.
     * 
     * @param port the port ti check.
     * @return <code>true</code> if the port supplies NMEA GPS data.
     * @throws Exception
     */
    public static boolean isThisAGpsPort( String port ) throws Exception {
        SerialPort comPort = SerialPort.getCommPort(port);
        comPort.openPort();
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        int waitTries = 0;
        int parseTries = 0;
        while( true && waitTries++ < 10 && parseTries < 2 ) {
            while( comPort.bytesAvailable() == 0 )
                Thread.sleep(500);

            byte[] readBuffer = new byte[comPort.bytesAvailable()];
            int numRead = comPort.readBytes(readBuffer, readBuffer.length);
            if (numRead > 0) {
                String data = new String(readBuffer);
                String[] split = data.split("\n");
                for( String line : split ) {
                    if (SentenceValidator.isSentence(line)) {
                        return true;
                    }
                }
                parseTries++;
            }
        }

        return false;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent( SerialPortEvent event ) {
        if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
            return;
        }
        byte[] newData = new byte[commPort.bytesAvailable()];
        commPort.readBytes(newData, newData.length);
        String dataString = new String(newData);

        if (logWriter != null)
            try {
                logWriter.write(dataString);
            } catch (IOException e) {
                e.printStackTrace();
            }

        if (!deviceChecked) {
            // check if it is an HM string
            if (dataString.contains(HM_START)) {
                isHmDevice = true;
                deviceChecked = true;
            } else {
                String[] lines = dataString.split("\n");
                for( String line : lines ) {
                    if (SentenceValidator.isValid(line)) {
                        isHmDevice = false;
                        deviceChecked = true;
                        break;
                    }
                }
            }
        }

        if (deviceChecked) {
            if (isHmDevice) {
                handleHmDataString(dataString);
            } else {
                handleDataString(dataString);
            }
        }
    }

    private void handleDataString( String dataString ) {
        String[] lines = dataString.split("\n");
        for( String line : lines ) {
            if (SentenceValidator.isValid(line)) {
                if (line.contains("MTK001")) {
                    continue;
                }
                ByteArrayInputStream source = new ByteArrayInputStream(line.getBytes());
                SentenceReader reader = new SentenceReader(source);
                reader.addSentenceListener(SerialNmeaGps.this, SentenceId.GSA);
                reader.addSentenceListener(SerialNmeaGps.this, SentenceId.GLL);
                reader.addSentenceListener(SerialNmeaGps.this, SentenceId.GGA);
                reader.addSentenceListener(SerialNmeaGps.this, SentenceId.GSV);
                reader.addSentenceListener(SerialNmeaGps.this, SentenceId.RMC);
                reader.start();
            }
        }
    }

    private void handleHmDataString( String dataString ) {
        hmDataString += dataString.trim();

        int firstHM = hmDataString.indexOf(HM_START);
        if (firstHM != -1) {
            int secondHM = hmDataString.indexOf(HM_START, firstHM + 1);
            if (secondHM != -1) {
//                System.out.println(hmDataString);
//                System.out.println("--------------------");
                String hmSentence = hmDataString.substring(firstHM, secondHM);
//                System.out.println(hmSentence);

                // set remainging to rest
                hmDataString = hmDataString.substring(secondHM);
//                System.out.println("--------------------");
//                System.out.println(hmSentence);
//                System.out.println("--------------------");
//                System.out.println(hmDataFString);
//                System.out.println("====================");

                // hortonmachine sensor gps
                // $HM:fixstatus,yy-MM-dd
                // hh:mm:ss,lat,lon,elev,heading,satcount,speedkmh,hdop,vdop,pdop,laterrM,lonerrM,alterrM,humidity%,tempC,pressurehPa
                System.out.println(hmSentence);
                hmSentence = hmSentence.substring(4);
                String[] dataSplit = hmSentence.split(",");
                if (dataSplit.length == 17) {
                    int i = 0;
                    String fixStatus = dataSplit[i++];
                    String ts = getTs(dataSplit[i++]);
                    double lat = getDouble(dataSplit[i++]);
                    double lon = getDouble(dataSplit[i++]);
                    double elev = getDouble(dataSplit[i++]);
                    double heading = getDouble(dataSplit[i++]);
                    int satCount = (int) getDouble(dataSplit[i++]);
                    double speedKmh = getDouble(dataSplit[i++]);
                    double hdop = getDouble(dataSplit[i++]);
                    double vdop = getDouble(dataSplit[i++]);
                    double pdop = getDouble(dataSplit[i++]);
                    double latErr = getDouble(dataSplit[i++]);
                    double lonErr = getDouble(dataSplit[i++]);
                    double altErr = getDouble(dataSplit[i++]);
                    double humidityPerc = getDouble(dataSplit[i++]);
                    double tempC = getDouble(dataSplit[i++]);
                    double pressure = getDouble(dataSplit[i++]);

                    currentGpsInfo.setHmGpsInfo(ts, lat, lon, elev, heading, satCount, speedKmh, hdop, vdop, pdop, latErr, lonErr,
                            altErr, humidityPerc, tempC, pressure);

                    for( NmeaGpsListener nmeaGpsListener : listeners ) {
                        nmeaGpsListener.onGpsEvent(currentGpsInfo);
                    }

                }

            }
        }

        String[] lines = dataString.split("\n");
        for( String line : lines ) {
            if (line.contains("HM:")) {
            }
        }
    }

    private String getTs( String string ) {
        if (string.equals("*")) {
            return "1970-01-01 00:00:00";
        }
        return string;
    }

    private double getDouble( String string ) {
        if (string.equals("*")) {
            return Double.NaN;
        }
        return Double.parseDouble(string);
    }

}
