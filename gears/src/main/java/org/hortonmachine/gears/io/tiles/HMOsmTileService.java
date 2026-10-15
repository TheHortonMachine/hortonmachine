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
package org.hortonmachine.gears.io.tiles;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.imageio.ImageIO;

import org.geotools.tile.Tile;
import org.geotools.tile.impl.osm.OSMService;
import org.hortonmachine.Version;

/**
 * An {@link OSMService} that downloads the tiles with the project's {@link Version#USER_AGENT}.
 *
 * <p>The GeoTools service is created without a http client, so its tiles fail to load, and
 * the GeoTools default client overwrites the User-Agent with a generic one, which the
 * OpenStreetMap tile servers block.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMOsmTileService extends OSMService {
    /**
     * The OpenStreetMap standard tiles.
     */
    public static final String OSM_BASE_URL = "https://tile.openstreetmap.org/";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * @param name the service name.
     * @param baseUrl the tiles base url, as for example {@link #OSM_BASE_URL}.
     */
    public HMOsmTileService( String name, String baseUrl ) {
        super(name, baseUrl);
    }

    @Override
    public BufferedImage loadImageTileImage( Tile tile ) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder(tile.getUrl().toURI()).header("User-Agent", Version.USER_AGENT)
                    .timeout(Duration.ofSeconds(20)).GET().build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new IOException("Http error " + response.statusCode() + " for tile: " + tile.getUrl());
            }
            return ImageIO.read(new ByteArrayInputStream(response.body()));
        } catch (URISyntaxException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
