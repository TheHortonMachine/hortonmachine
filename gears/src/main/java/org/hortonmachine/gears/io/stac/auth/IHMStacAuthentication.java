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

/**
 * An authentication method to access protected stac catalogs and assets.
 *
 * <p>Implementations decide which addresses they apply to (for example the hosts or buckets
 * they have credentials for) and perform the authenticated requests, so that catalog documents,
 * items and assets can all be read through the same {@link HMStacAccess}.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public interface IHMStacAuthentication {

    /**
     * @param href the address of a catalog document or asset.
     * @return <code>true</code> if this authentication has to be used to access the address.
     */
    boolean appliesTo( String href );

    /**
     * Convert the address to one that can be used by http clients, if needed
     * (for example <code>s3://bucket/key</code> to the https url of the object).
     *
     * @param href the address.
     * @return the http(s) address.
     */
    default String toHttpUrl( String href ) {
        return href;
    }

    /**
     * Perform an authenticated GET request.
     *
     * @param href the address to read.
     * @return the response, to be closed after use.
     * @throws IOException if the request fails.
     */
    HMStacResponse get( String href ) throws IOException;
}
