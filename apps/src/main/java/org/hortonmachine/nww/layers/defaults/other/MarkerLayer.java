/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package org.hortonmachine.nww.layers.defaults.other;

import java.util.Iterator;
import java.util.List;

import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.markers.Marker;
import gov.nasa.worldwind.render.markers.MarkerRenderer;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.Logging;

/**
 * @author tag
 * @version $Id: MarkerLayer.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class MarkerLayer extends AbstractLayer {
    private MarkerRenderer markerRenderer = new MarkerRenderer();
    private List<Marker> markers;
    private int maxMarkers = -1;
    private volatile int markersCount = 0;

    public MarkerLayer() {
    }

    public MarkerLayer(List<Marker> markers) {
        this.markers = markers;
        markersCount = markers.size();
    }

    public List<Marker> getMarkers() {
        return markers;
    }

    public void setMarkers(List<Marker> markers) {
        this.markers = markers;
        markersCount = markers.size();
    }

    public void addMarker(Marker marker) {
        synchronized (markers) {
            markers.add(marker);
            markersCount++;
            if (maxMarkers != -1 && markersCount > maxMarkers) {
                // remove previous markers
                int toRemove = markersCount - maxMarkers;
                Iterator<Marker> iterator = markers.iterator();
                int removeCount = 0;
                while (iterator.hasNext()) {
                    iterator.next();
                    iterator.remove();
                    markersCount--;
                    if (removeCount++ >= toRemove) {
                        break;
                    }
                }
            }
        }
    }

    public void setMaxMarkers(int maxMarkers) {
        this.maxMarkers = maxMarkers;
    }

    public double getElevation() {
        return this.getMarkerRenderer().getElevation();
    }

    public void setElevation(double elevation) {
        this.getMarkerRenderer().setElevation(elevation);
    }

    public boolean isOverrideMarkerElevation() {
        return this.getMarkerRenderer().isOverrideMarkerElevation();
    }

    public void setOverrideMarkerElevation(boolean overrideMarkerElevation) {
        this.getMarkerRenderer().setOverrideMarkerElevation(overrideMarkerElevation);
    }

    public boolean isKeepSeparated() {
        return this.getMarkerRenderer().isKeepSeparated();
    }

    public void setKeepSeparated(boolean keepSeparated) {
        this.getMarkerRenderer().setKeepSeparated(keepSeparated);
    }

    public boolean isEnablePickSizeReturn() {
        return this.getMarkerRenderer().isEnablePickSizeReturn();
    }

    public void setEnablePickSizeReturn(boolean enablePickSizeReturn) {
        this.getMarkerRenderer().setEnablePickSizeReturn(enablePickSizeReturn);
    }

    /**
     * Opacity is not applied to layers of this type because each marker has an
     * attribute set with opacity control.
     *
     * @param opacity
     *            the current opacity value, which is ignored by this layer.
     */
    @Override
    public void setOpacity(double opacity) {
        super.setOpacity(opacity);
    }

    /**
     * Returns the layer's opacity value, which is ignored by this layer because
     * each of its markers has an attribute with its own opacity control.
     *
     * @return The layer opacity, a value between 0 and 1.
     */
    @Override
    public double getOpacity() {
        return super.getOpacity();
    }

    protected MarkerRenderer getMarkerRenderer() {
        return markerRenderer;
    }

    protected void setMarkerRenderer(MarkerRenderer markerRenderer) {
        this.markerRenderer = markerRenderer;
    }

    protected void doRender(DrawContext dc) {
        this.draw(dc, null);
    }

    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint) {
        this.draw(dc, pickPoint);
    }

    protected void draw(DrawContext dc, java.awt.Point pickPoint) {
        if (this.markers == null)
            return;

        if (dc.getVisibleSector() == null)
            return;

        SectorGeometryList geos = dc.getSurfaceGeometry();
        if (geos == null)
            return;

        // Adds markers to the draw context's ordered renderable queue. During
        // picking, this gets
        // the pick point and the
        // current layer from the draw context.
        this.getMarkerRenderer().render(dc, this.markers);
    }

    @Override
    public String toString() {
        return Logging.getMessage("layers.MarkerLayer.Name");
    }
}
