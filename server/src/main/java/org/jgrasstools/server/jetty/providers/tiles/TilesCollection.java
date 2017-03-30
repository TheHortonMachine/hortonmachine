/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.server.jetty.providers.tiles;

/**
 * Small collection of tiles sources to use for tests.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class TilesCollection {

    private OsmOLProvider osmProvider;
    private WmsProvider aerialAltoAdigeProvider;
    private WmsProvider ctpTrentinoProvider;
    private WmsProvider aerialEmiliaProvider;
    private WmsProvider ctrEmiliaProvider;
    private BingProvider roadsProvider;
    private BingProvider aerialWithLabelsProvider;

    public TilesCollection() {
        osmProvider = new OsmOLProvider("OpenStreetMap");

        aerialAltoAdigeProvider = new WmsProvider("Aerial Alto Adige", "http://sdi.provincia.bz.it/geoserver/wms",
                "'inspire:OI.ORTHOIMAGECOVERAGE.2011'", "Copyright Alto Adige",
                "{'LAYERS': 'inspire:OI.ORTHOIMAGECOVERAGE.2011', 'TILED': true}", "geoserver");
        ctpTrentinoProvider = new WmsProvider("CTP Trentino",
                "http://geoservices.provincia.tn.it/siat/services/OGC/CTP2013/ImageServer/WMSServer", "'0'", "Copyright Trentino",
                "{'LAYERS': '0', 'TILED': true}", "geoserver");
        aerialEmiliaProvider = new WmsProvider("Aerial Emilia 2011",
                "http://servizigis.regione.emilia-romagna.it/wms/agea2011_rgb", "'public/Agea2011_RGB'",
                "Copyright Emilia Romagna", "{'LAYERS': 'public/Agea2011_RGB', 'TILED': true}", "geoserver");
        ctrEmiliaProvider = new WmsProvider("CTR Emilia 2013", "http://servizigis.regione.emilia-romagna.it/wms/dbtr2013_ctr5",
                "'public/DBTR2013_Ctr5'", "Copyright Emilia Romagna", "{'LAYERS': 'public/DBTR2013_Ctr5', 'TILED': true}",
                "geoserver");

        roadsProvider = new BingProvider("Bing roads", "Road", "Your Bing Maps Key from http://www.bingmapsportal.com/ here");
        aerialWithLabelsProvider = new BingProvider("Bing Aerial", "AerialWithLabels",
                "Your Bing Maps Key from http://www.bingmapsportal.com/ here");
    }

    public OsmOLProvider getOsmProvider() {
        return osmProvider;
    }

    public WmsProvider getAerialAltoAdigeProvider() {
        return aerialAltoAdigeProvider;
    }

    public WmsProvider getCtpTrentinoProvider() {
        return ctpTrentinoProvider;
    }

    public WmsProvider getAerialEmiliaProvider() {
        return aerialEmiliaProvider;
    }

    public WmsProvider getCtrEmiliaProvider() {
        return ctrEmiliaProvider;
    }

    public BingProvider getRoadsProvider() {
        return roadsProvider;
    }

    public BingProvider getAerialWithLabelsProvider() {
        return aerialWithLabelsProvider;
    }

}
