/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
 */
package org.hortonmachine.ssh;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * Proxy handler.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ProxyEnabler {

    private static String _url;
    private static String _port;
    private static String _user;
    private static String _pwd;
    private static boolean hasProxy = false;

    /**
     * Enable the proxy usage based on the url, user and pwd.
     * 
     * @param url the proxy server url.
     * @param port the server port.
     * @param user the proxy user.
     * @param pwd the proxy password.
     * @param nonProxyHosts hosts that do not go through proxy.
     *          <p>Default is: <code>localhost|127.*|[::1]</code>.</p>
     *          <p>Other example: *.foo.com|localhost</p>
     */
    public static void enableProxy( String url, String port, String user, String pwd, String nonProxyHosts ) {
        _url = url;
        _port = port;
        _user = user;
        _pwd = pwd;
        System.setProperty("http.proxyHost", url);
        System.setProperty("https.proxyHost", url);

        if (port != null && port.trim().length() != 0) {
            System.setProperty("http.proxyPort", port);
            System.setProperty("https.proxyPort", port);
        }

        if (user != null && pwd != null && user.trim().length() != 0 && pwd.trim().length() != 0) {
            System.setProperty("http.proxyUserName", user);
            System.setProperty("https.proxyUserName", user);
            System.setProperty("http.proxyUser", user);
            System.setProperty("https.proxyUser", user);
            System.setProperty("http.proxyPassword", pwd);
            System.setProperty("https.proxyPassword", pwd);

            Authenticator.setDefault(new ProxyAuthenticator(user, pwd));

        }

        if (nonProxyHosts != null) {
            System.setProperty("http.nonProxyHosts", nonProxyHosts);
        }
        hasProxy = true;
    }

    /**
     * On recent Windows systems and on Gnome 2.x systems it is possible 
     * to tell the java.net stack, setting this property to true, to use 
     * the system proxy settings (both  these systems let you set proxies 
     * globally through their user interface). Note that this property is 
     * checked only once at startup.
     */
    public static void useSystemProxy() {
        System.setProperty("java.net.useSystemProxies", "true");
    }

    /**
     * Disable the proxy usage.
     */
    public static void disableProxy() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("http.proxyUserName");
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        System.clearProperty("https.proxyUserName");
        System.clearProperty("https.proxyUser");
        System.clearProperty("https.proxyPassword");

        _url = null;
        _port = null;
        _user = null;
        _pwd = null;
        hasProxy = false;
    }

    public static String getUrl() {
        return _url;
    }

    public static String getPort() {
        return _port;
    }

    public static int getPortInt() {
        return Integer.parseInt(_port);
    }

    public static String getUser() {
        return _user;
    }

    public static String getPwd() {
        return _pwd;
    }

    public static boolean hasProxy() {
        return hasProxy;
    }

    static class ProxyAuthenticator extends Authenticator {

        private String user, password;

        public ProxyAuthenticator( String user, String password ) {
            this.user = user;
            this.password = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, password.toCharArray());
        }
    }
}
