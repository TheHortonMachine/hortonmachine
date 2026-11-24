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
package org.hortonmachine.gears.io.stac.cog;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.function.Function;

import javax.imageio.stream.ImageInputStream;

import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.hortonmachine.gears.io.stac.cog.HMRangeReader.RangeFetcher;

import it.geosolutions.imageio.core.BasicAuthURI;
import it.geosolutions.imageio.plugins.cog.CogImageReadParam;
import it.geosolutions.imageioimpl.plugins.cog.CogImageInputStream;
import it.geosolutions.imageioimpl.plugins.cog.CogImageInputStreamSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogImageReaderSpi;
import it.geosolutions.imageioimpl.plugins.cog.CogSourceSPIProvider;
import it.geosolutions.imageioimpl.plugins.cog.HttpRangeReader;

/**
 * The source of a cloud optimized GeoTIFF for the GeoTools readers, read with range requests through
 * an {@link HMRangeReader}, as the authenticated requests of an {@link HMStacAccess}.
 *
 * <pre>
 * GeoTiffReader reader = new GeoTiffReader(HMCogSourceSPIProvider.fromAccess(access, href, endpoint, region));
 * </pre>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMCogSourceSPIProvider extends CogSourceSPIProvider {
    private final String href;
    private final String httpUrl;
    private final Function<String, RangeFetcher> fetcherFactory;

    /**
     * @param href the address of the object, as s3://bucket/key.
     * @param httpUrl the http(s) address of the object, used only to name it.
     * @param fetcherFactory the reader of the bytes for an address, the one of the object or of other files
     *          next to it, as the external overviews.
     */
    public HMCogSourceSPIProvider( String href, String httpUrl, Function<String, RangeFetcher> fetcherFactory ) {
        super(new BasicAuthURI(URI.create(httpUrl), false), new CogImageReaderSpi(), new CogImageInputStreamSpi(),
                HttpRangeReader.class.getName());
        this.href = href;
        this.httpUrl = httpUrl;
        this.fetcherFactory = fetcherFactory;
    }

    /**
     * Create the source of an object read through an access.
     *
     * @param access the access.
     * @param href the address of the object.
     * @param storageEndpoint the endpoint of the S3 compatible service holding it or null.
     * @param storageRegion the region of its bucket or null.
     * @return the source.
     */
    public static HMCogSourceSPIProvider fromAccess( HMStacAccess access, String href, String storageEndpoint,
            String storageRegion ) {
        String httpUrl = access.toHttpUrl(href, storageEndpoint, storageRegion);
        return new HMCogSourceSPIProvider(href, httpUrl,
                h -> ( start, end ) -> access.getRange(h, storageEndpoint, storageRegion, start, end));
    }

    /**
     * @return the address of the object.
     */
    public String getHref() {
        return href;
    }

    /**
     * @return a new range reader of the object.
     */
    public HMRangeReader createRangeReader() {
        return new HMRangeReader(getCogUri(), CogImageReadParam.DEFAULT_HEADER_LENGTH, fetcherFactory.apply(href));
    }

    @Override
    public URL getSourceUrl() throws MalformedURLException {
        return URI.create(httpUrl).toURL();
    }

    @Override
    public ImageInputStream getStream() throws IOException {
        CogImageInputStream stream = (CogImageInputStream) ((CogImageInputStreamSpi) getStreamSpi())
                .createInputStreamInstance(getCogUri(), false, null);
        try {
            stream.init(createRangeReader());
        } catch (RuntimeException e) {
            stream.close();
            throw new IOException(e.getMessage(), e);
        }
        return stream;
    }

    @Override
    public CogSourceSPIProvider getCompatibleSourceProvider( URL url ) {
        // as the external overviews: href.ovr for httpUrl.ovr
        String other = url.toString();
        String otherHref = other.startsWith(httpUrl) ? href + other.substring(httpUrl.length()) : other;
        HMCogSourceSPIProvider provider = new HMCogSourceSPIProvider(otherHref, other, fetcherFactory);
        provider.setReaderSpi(getReaderSpi());
        return provider;
    }
}
