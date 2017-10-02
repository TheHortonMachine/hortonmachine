package org.hortonmachine.dbs.h2gis;

public class H2GisServer {

    public static void main( String[] args ) throws Exception {

        int port = 9092;
        boolean doWeb = false;

        for( String arg : args ) {
            if (arg.startsWith("port=")) {
                String portStr = arg.replaceFirst("port=", "");
                port = Integer.parseInt(portStr);
            } else if (arg.startsWith("type=")) {
                String typeStr = arg.replaceFirst("type=", "");
                if (typeStr.equalsIgnoreCase("web")) {
                    doWeb = true;
                }
            }
        }

        if (doWeb) {
            H2GisDb.startWebServerMode(String.valueOf(port), true, true, null);
        } else {
            H2GisDb.startTcpServerMode(String.valueOf(port), false, null, true, null);
        }

    }

}
