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
package org.hortonmachine.gears.io.wfs;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.geotools.util.EntityResolver3;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.factory.Hints;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * An entity resolver that allows the remote schemas of the connected WFS services.
 *
 * <p>Since GeoTools 34 the default entity resolver only allows the schemas of a few well known
 * hosts (OGC, INSPIRE, W3C). The GML returned by a GetFeature references the
 * DescribeFeatureType of the service itself in its schemaLocation and the GetFeature
 * pull parser takes the resolver from the global GeoTools hints (the
 * <code>ENTITY_RESOLVER</code> parameter of the WFS datastore does not reach it). With the
 * schema blocked the features can't be built and the parsing fails.</p>
 *
 * <p>This resolver wraps the resolver that is the default at install time and additionally
 * allows http(s) entities of the hosts registered through {@link #allowHost(String)}. Everything
 * else is delegated, so the protection against local file access stays in place.</p>
 *
 * <p>It has to be an {@link EntityResolver3}: for other resolvers the GeoTools StAX factory
 * setup sets the external schema access property to a Boolean instead of a String, which
 * makes the JDK parser fail with a ClassCastException.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
class WfsEntityResolver implements EntityResolver3 {

    private static final Set<String> ALLOWED_HOSTS = ConcurrentHashMap.newKeySet();
    private static WfsEntityResolver installed;

    private final EntityResolver delegate;

    private WfsEntityResolver( EntityResolver delegate ) {
        this.delegate = delegate;
    }

    /**
     * Allows the remote entities of the host of the given service url and makes sure the
     * resolver is installed as GeoTools default.
     *
     * @param serviceUrl the url of the service.
     */
    static void allowHost( String serviceUrl ) {
        String host = hostOf(serviceUrl);
        if (host != null) {
            ALLOWED_HOSTS.add(host);
        }
        install();
    }

    private static synchronized void install() {
        EntityResolver current = GeoTools.getEntityResolver(null);
        if (installed != null && current == installed) {
            return;
        }
        installed = new WfsEntityResolver(current);
        Hints.putSystemDefault(Hints.ENTITY_RESOLVER, installed);
    }

    private static String hostOf( String url ) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null) {
                return null;
            }
            scheme = scheme.toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return null;
            }
            String host = uri.getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isAllowed( String baseURI, String systemId ) {
        if (systemId == null) {
            return false;
        }
        String absolute = systemId;
        if (baseURI != null) {
            try {
                absolute = new URI(baseURI).resolve(new URI(systemId)).toString();
            } catch (Exception e) {
                // keep the system id as is
            }
        }
        String host = hostOf(absolute);
        return host != null && ALLOWED_HOSTS.contains(host);
    }

    /**
     * The protocols the parsers may open, the ones of the delegate plus http (which covers https).
     */
    @Override
    public String getAccess() {
        String access = delegate instanceof EntityResolver3 ? ((EntityResolver3) delegate).getAccess() : null;
        if (access == null || access.isBlank()) {
            return "http";
        }
        if (access.contains("all") || access.contains("http")) {
            return access;
        }
        return access + ",http";
    }

    @Override
    public InputSource resolveEntity( String publicId, String systemId ) throws SAXException, IOException {
        if (isAllowed(null, systemId)) {
            // null means: let the parser open the remote resource
            return null;
        }
        return delegate.resolveEntity(publicId, systemId);
    }

    @Override
    public InputSource resolveEntity( String name, String publicId, String baseURI, String systemId )
            throws SAXException, IOException {
        if (isAllowed(baseURI, systemId)) {
            return null;
        }
        if (delegate instanceof EntityResolver2) {
            return ((EntityResolver2) delegate).resolveEntity(name, publicId, baseURI, systemId);
        }
        return delegate.resolveEntity(publicId, systemId);
    }

    @Override
    public InputSource getExternalSubset( String name, String baseURI ) throws SAXException, IOException {
        if (delegate instanceof EntityResolver2) {
            return ((EntityResolver2) delegate).getExternalSubset(name, baseURI);
        }
        return null;
    }

    @Override
    public String toString() {
        return "WfsEntityResolver[hosts=" + ALLOWED_HOSTS + ", delegate=" + delegate + "]";
    }
}
