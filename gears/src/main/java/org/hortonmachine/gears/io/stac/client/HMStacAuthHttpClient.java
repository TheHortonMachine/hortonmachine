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
package org.hortonmachine.gears.io.stac.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import org.geotools.http.DelegateHTTPClient;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPResponse;
import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.hortonmachine.gears.io.stac.auth.HMStacResponse;
import org.hortonmachine.gears.io.stac.auth.IHMStacAuthentication;

/**
 * An http client that performs the GET requests through the authentications of a {@link HMStacAccess}
 * when they apply, delegating all other requests.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMStacAuthHttpClient extends DelegateHTTPClient {
    private final HMStacAccess access;

    public HMStacAuthHttpClient( HTTPClient delegate, HMStacAccess access ) {
        super(delegate);
        this.access = access;
    }

    @Override
    public HTTPResponse get( URL url ) throws IOException {
        IHMStacAuthentication authentication = access.getAuthentication(url.toString());
        if (authentication != null) {
            return new AuthResponse(authentication.get(url.toString()));
        }
        return super.get(url);
    }

    @Override
    public HTTPResponse get( URL url, Map<String, String> headers ) throws IOException {
        IHMStacAuthentication authentication = access.getAuthentication(url.toString());
        if (authentication != null) {
            return new AuthResponse(authentication.get(url.toString()));
        }
        return super.get(url, headers);
    }

    private static class AuthResponse implements HTTPResponse {
        private final HMStacResponse response;

        AuthResponse( HMStacResponse response ) {
            this.response = response;
        }

        @Override
        public void dispose() {
            try {
                response.close();
            } catch (IOException e) {
                // ignore
            }
        }

        @Override
        public String getContentType() {
            return response.getContentType();
        }

        @Override
        public String getResponseHeader( String headerName ) {
            if ("Content-Type".equalsIgnoreCase(headerName))
                return response.getContentType();
            if ("Content-Length".equalsIgnoreCase(headerName) && response.getContentLength() >= 0)
                return String.valueOf(response.getContentLength());
            return null;
        }

        @Override
        public InputStream getResponseStream() throws IOException {
            return response.getInputStream();
        }

        @Override
        public String getResponseCharset() {
            String contentType = response.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("charset=")) {
                return contentType.substring(contentType.toLowerCase().indexOf("charset=") + 8).trim();
            }
            return null;
        }
    }
}
