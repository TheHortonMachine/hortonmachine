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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * The successful response of a request done through {@link HMStacAccess}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMStacResponse implements Closeable {
    private final InputStream inputStream;
    private final String contentType;
    private final long contentLength;

    /**
     * @param inputStream the content.
     * @param contentType the content type or null if not known.
     * @param contentLength the length or -1 if not known.
     */
    public HMStacResponse( InputStream inputStream, String contentType, long contentLength ) {
        this.inputStream = inputStream;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    /**
     * @return the content type or null if not known.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return the content length or -1 if not known.
     */
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
