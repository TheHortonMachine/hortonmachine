package org.jgrasstools.server.jetty.utils;

import javax.servlet.http.HttpServletRequest;



/**
 * Network utils methods.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class NetworkUtilities {

    public static String getIpAddress( HttpServletRequest request ) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }


}