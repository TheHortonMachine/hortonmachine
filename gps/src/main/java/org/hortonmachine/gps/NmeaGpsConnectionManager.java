package org.hortonmachine.gps;

import java.util.concurrent.ArrayBlockingQueue;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import net.sf.marineapi.nmea.sentence.SentenceValidator;

public enum NmeaGpsConnectionManager {
    INSTANCE;

    private int parity = SerialPort.PARITY_NONE;
    private int stopBits = SerialPort.STOPBITS_1;
    private int dataBits = SerialPort.DATABITS_8;
    private int baudRate = SerialPort.BAUDRATE_9600;

    private ArrayBlockingQueue<String> portProcessingQueue = new ArrayBlockingQueue<>(10);

    interface GpsPortCheckListener {
        void onGpsPortFound( String port );

        void onPortError( String portName, SerialPortException ex );
    }

    public String[] getAvailablePorts() {
        String[] portNames = SerialPortList.getPortNames();
        return portNames;
    }

    public void setPortParameters( int baudRate, int dataBits, int stopBits, int parity ) {
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;
    }

    public void findNmeaGpsPort( GpsPortCheckListener gpsPortCheckListener ) {
        portProcessingQueue.clear();
        String[] availablePorts = getAvailablePorts();
        for( String port : availablePorts ) {
            portProcessingQueue.add(port);
        }

        checkNextPort(gpsPortCheckListener);
    }

    public SerialPort getConnection( String port ) throws SerialPortException {
        SerialPort serialPort = new SerialPort(port);
        boolean openPort = serialPort.openPort();
        if (openPort) {
            serialPort.setParams(baudRate, dataBits, stopBits, parity);
            return serialPort;
        }
        return null;
    }

    public void closeConnection( SerialPort serialPort ) throws SerialPortException {
        if (serialPort != null && serialPort.isOpened())
            serialPort.closePort();
    }

    /**
     * A blocking connection to all available ports to see if a GPS is connected.
     * 
     * @return the port name or <code>null</code> if no GPS was found.
     */
    public String findNmeaGpsPort() {
        String[] availablePorts = getAvailablePorts();
        for( String port : availablePorts ) {
            if (port != null) {
                SerialPort serialPort = new SerialPort(port);
                try {
                    if (serialPort.isOpened()) {
                        serialPort.closePort();
                    }
                    boolean openPort = serialPort.openPort();
                    if (openPort) {
                        serialPort.setParams(baudRate, dataBits, stopBits, parity);
                        String readString = serialPort.readString();
                        if (readString == null) {
                            continue;
                        }
                        String[] split = readString.split("\n");
                        for( String line : split ) {
                            boolean isSentence = SentenceValidator.isSentence(line);
                            if (isSentence) {
                                if (serialPort != null && serialPort.isOpened())
                                    try {
                                        serialPort.closePort();
                                    } catch (SerialPortException e) {
                                        e.printStackTrace();
                                    }
                                return port;
                            }
                        }
                    }
                } catch (SerialPortException e) {
                    e.printStackTrace();
                } finally {
                    if (serialPort != null && serialPort.isOpened())
                        try {
                            serialPort.closePort();
                        } catch (SerialPortException e) {
                            e.printStackTrace();
                        }
                }
            }
        }

        return null;
    }
    private void checkNextPort( GpsPortCheckListener gpsPortCheckListener ) {
        String port = portProcessingQueue.poll();
        if (port != null) {
            System.out.println("Check port: " + port);
            SerialPort serialPort = new SerialPort(port);
            try {
                boolean openPort = serialPort.openPort();
                if (openPort) {
                    System.out.println("Opened port: " + port);
                    serialPort.setParams(baudRate, dataBits, stopBits, parity);
//                int mask = SerialPort.MASK_RXCHAR;
//                serialPort.setEventsMask(mask);
                    serialPort.addEventListener(new SerialPortEventListener(){
                        @Override
                        public void serialEvent( SerialPortEvent event ) {
                            if (event.isRXCHAR() && event.getEventValue() > 0) {
                                try {
                                    String receivedData = serialPort.readString(event.getEventValue());

                                    boolean isSentence = SentenceValidator.isSentence(receivedData);
                                    boolean isValid = SentenceValidator.isValid(receivedData);

                                    System.out.println("Is sentence: " + isSentence);
                                    System.out.println("Is valid: " + isValid);

                                    if (isSentence && isValid) {
                                        gpsPortCheckListener.onGpsPortFound(serialPort.getPortName());
                                    } else {
                                        serialPort.closePort();
                                        checkNextPort(gpsPortCheckListener);
                                    }
                                } catch (SerialPortException ex) {
                                    gpsPortCheckListener.onPortError(serialPort.getPortName(), ex);
                                    if (serialPort != null && serialPort.isOpened())
                                        try {
                                            serialPort.closePort();
                                        } catch (SerialPortException e) {
                                            e.printStackTrace();
                                        }
                                    checkNextPort(gpsPortCheckListener);
                                }
                            }
                        }
                    });
                }
            } catch (SerialPortException e) {
                e.printStackTrace();
            } finally {
                if (serialPort != null && serialPort.isOpened())
                    try {
                        serialPort.closePort();
                    } catch (SerialPortException e) {
                        e.printStackTrace();
                    }
            }
            System.out.println("Done port: " + port);
        }
    }

}
