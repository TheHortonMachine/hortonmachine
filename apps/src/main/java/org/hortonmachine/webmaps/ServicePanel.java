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
package org.hortonmachine.webmaps;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.locationtech.jts.geom.Envelope;

/**
 * The part of the {@link WebServicesBrowser} specific to a service type: it connects to the service, lists its
 * layers and provides the request options, the actions and the panel showing the results.
 *
 * <p>Each service type keeps its own state, so switching type and back shows the last connection again.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
abstract class ServicePanel {

    /** A layer, coverage or feature type of a service. */
    static class LayerEntry {
        final String name;
        String title;
        /** The extent in WGS84 lon/lat, null if not known (yet). */
        Envelope wgs84;
        Object data;

        LayerEntry( String name, String title, Envelope wgs84, Object data ) {
            this.name = name;
            this.title = title;
            this.wgs84 = wgs84;
            this.data = data;
        }
    }

    protected final WebServicesBrowser browser;
    protected List<LayerEntry> entries = new ArrayList<>();
    protected LayerEntry currentEntry;
    protected String connectedUrl;
    protected String serviceInfoHtml;
    protected String layerInfoHtml;

    ServicePanel( WebServicesBrowser browser ) {
        this.browser = browser;
    }

    /** @return the service type, e.g. WMS. */
    abstract String getType();

    /** @return the versions that can be forced, the first being the automatic negotiation. */
    abstract String[] getVersions();

    /** @return some public services to try. */
    abstract String[] getPresets();

    /** @return the options of the requests, shown next to the map. */
    abstract JComponent getRequestPanel();

    /** @return the buttons running the requests, always visible below the options. */
    abstract JComponent getActionPanel();

    /** @return the panel showing the results, in the Result tab. */
    abstract JComponent getResultPanel();

    /**
     * Connect to a service. Runs in background, must not touch the ui.
     *
     * @return the layers of the service.
     */
    abstract List<LayerEntry> connect( String url, String version ) throws Exception;

    /** Called on the EDT after a successful {@link #connect(String, String)}. */
    void connected() {
    }

    /** Called on the EDT when a layer is selected in the list. */
    abstract void selectLayer( LayerEntry entry );

    /** Called on the EDT when the region of the requests changes. */
    void regionChanged() {
    }

    /** Called when a footprint is clicked on the map (-1 for none). */
    void footprintSelected( int index ) {
    }

    /** Enable or disable the actions while a task runs. */
    void setBusy( boolean busy ) {
    }

    /** Release the connection. */
    void close() {
    }

    void setLayerInfo( String html ) {
        layerInfoHtml = html;
        browser.showLayerInfo(html);
    }
}
