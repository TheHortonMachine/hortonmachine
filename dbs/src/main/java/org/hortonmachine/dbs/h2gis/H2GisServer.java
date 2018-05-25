package org.hortonmachine.dbs.h2gis;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.h2.tools.Server;

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
            startWebServerMode(String.valueOf(port), true, true, null);
        } else {
            startTcpServerMode(String.valueOf(port), false, null, true, null);
        }

    }

    /**
     * Start the server mode.
     * 
     * <p>This calls:
     *<pre>
     * Server server = Server.createTcpServer(
     *     "-tcpPort", "9123", "-tcpAllowOthers").start();
     * </pre>
     * Supported options are:
     * -tcpPort, -tcpSSL, -tcpPassword, -tcpAllowOthers, -tcpDaemon,
     * -trace, -ifExists, -baseDir, -key.
     * See the main method for details.
     * <p>
     * 
     * @param port the optional port to use.
     * @param doSSL if <code>true</code>, ssl is used.
     * @param tcpPassword an optional tcp passowrd to use.
     * @param ifExists is <code>true</code>, the database to connect to has to exist.
     * @param baseDir an optional basedir into which it is allowed to connect.
     * @return
     * @throws SQLException
     */
    public static Server startTcpServerMode( String port, boolean doSSL, String tcpPassword, boolean ifExists, String baseDir )
            throws SQLException {
        List<String> params = new ArrayList<>();
        params.add("-tcpAllowOthers");
        params.add("-tcpPort");
        if (port == null) {
            port = "9123";
        }
        params.add(port);

        if (doSSL) {
            params.add("-tcpSSL");
        }
        if (tcpPassword != null) {
            params.add("-tcpPassword");
            params.add(tcpPassword);
        }

        if (ifExists) {
            params.add("-ifExists");
        }

        if (baseDir != null) {
            params.add("-baseDir");
            params.add(baseDir);
        }

        Server server = Server.createTcpServer(params.toArray(new String[0])).start();
        return server;
    }

    /**
     * Start the web server mode.
     * 
     * @param port the optional port to use.
     * @param doSSL if <code>true</code>, ssl is used.
     * @param ifExists is <code>true</code>, the database to connect to has to exist.
     * @param baseDir an optional basedir into which it is allowed to connect.
     * @return
     * @throws SQLException
     */
    public static Server startWebServerMode( String port, boolean doSSL, boolean ifExists, String baseDir ) throws SQLException {
        List<String> params = new ArrayList<>();
        params.add("-webAllowOthers");
        if (port != null) {
            params.add("-webPort");
            params.add(port);
        }

        if (doSSL) {
            params.add("-webSSL");
        }

        if (ifExists) {
            params.add("-ifExists");
        }

        if (baseDir != null) {
            params.add("-baseDir");
            params.add(baseDir);
        }

        Server server = Server.createWebServer(params.toArray(new String[0])).start();
        return server;
    }

}
