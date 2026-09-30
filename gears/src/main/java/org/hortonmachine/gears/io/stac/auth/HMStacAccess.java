/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.io.stac.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * The access context of a stac catalog: the authentications to use for the protected addresses.
 *
 * <p>The first authentication that applies to an address is used, addresses no authentication
 * applies to are read without authentication.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMStacAccess {
    private final List<IHMStacAuthentication> authentications = new ArrayList<>();

    public HMStacAccess() {
    }

    public HMStacAccess( IHMStacAuthentication... authentications ) {
        for( IHMStacAuthentication authentication : authentications ) {
            addAuthentication(authentication);
        }
    }

    public HMStacAccess addAuthentication( IHMStacAuthentication authentication ) {
        authentications.add(authentication);
        return this;
    }

    public List<IHMStacAuthentication> getAuthentications() {
        return authentications;
    }

    /**
     * @param href the address.
     * @return the authentication to use for the address or null if none applies.
     */
    public IHMStacAuthentication getAuthentication( String href ) {
        if (href == null)
            return null;
        for( IHMStacAuthentication authentication : authentications ) {
            if (authentication.appliesTo(href))
                return authentication;
        }
        return null;
    }

    /**
     * @param href the address.
     * @return <code>true</code> if the address needs authentication.
     */
    public boolean isAuthenticated( String href ) {
        return getAuthentication(href) != null;
    }

    /**
     * @param href the address.
     * @return the address usable by http clients (e.g. s3 urls converted to https).
     */
    public String toHttpUrl( String href ) {
        IHMStacAuthentication authentication = getAuthentication(href);
        return authentication != null ? authentication.toHttpUrl(href) : href;
    }

    /**
     * Read an address, with authentication if needed.
     *
     * @param href the address.
     * @return the response, to be closed after use.
     * @throws IOException if the request fails.
     */
    public HMStacResponse get( String href ) throws IOException {
        IHMStacAuthentication authentication = getAuthentication(href);
        if (authentication != null) {
            return authentication.get(href);
        }
        URLConnection connection = new URL(href).openConnection();
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(60_000);
        if (connection instanceof HttpURLConnection httpConnection) {
            int code = httpConnection.getResponseCode();
            if (code >= 400) {
                throw new IOException("HTTP " + code + " reading " + href);
            }
        }
        return new HMStacResponse(connection.getInputStream(), connection.getContentType(), connection.getContentLengthLong());
    }
}
