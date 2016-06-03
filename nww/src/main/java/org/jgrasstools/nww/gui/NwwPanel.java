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
package org.jgrasstools.nww.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.nww.layers.OSMMapnikLayer;
import org.jgrasstools.nww.utils.NwwUtilities;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.globes.GeographicProjection;
import gov.nasa.worldwind.globes.projections.ProjectionEquirectangular;
import gov.nasa.worldwind.globes.projections.ProjectionMercator;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.util.StatusBar;
import gov.nasa.worldwind.view.orbit.OrbitView;

/**
 * The main NWW panel.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NwwPanel extends JPanel {

    private WorldWindow wwd;
    protected StatusBar statusBar;

    public NwwPanel() {
        super(new BorderLayout());

        // Configuration.setValue(AVKey.INITIAL_LATITUDE, gpsLogShps[0].y);
        // Configuration.setValue(AVKey.INITIAL_LONGITUDE, gpsLogShps[0].x);
        // Configuration.setValue(AVKey.INITIAL_ALTITUDE, 1000);
        // Configuration.setValue(AVKey.INITIAL_PITCH, 45);

        wwd = new WorldWindowGLJPanel();
        ((Component) wwd).setPreferredSize(new Dimension(500, 500));

        Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        this.getWwd().setModel(model);

        LayerList layers = model.getLayers();
        List<Layer> addBack = new ArrayList<>();
        Iterator<Layer> layerIterator = layers.iterator();
        List<String> namesToKeep = NwwUtilities.LAYERS_TO_KEEP_FROM_ORIGNALNWW;
        while (layerIterator.hasNext()) {
            Layer layer = layerIterator.next();
            if (namesToKeep.contains(layer.getName())) {
                addBack.add(layer);
            }
        }
        layers.clear();

        // Create and install the view controls layer and register a controller
        // for it with the
        // World Window.
        ViewControlsLayer viewControlsLayer = new ViewControlsLayer();
        layers.add(viewControlsLayer);
        getWwd().addSelectListener(new ViewControlsSelectListener(getWwd(), viewControlsLayer));

        layers.addAll(addBack);

        layers.add(new OSMMapnikLayer());

        this.add((Component) this.getWwd(), BorderLayout.CENTER);
        this.statusBar = new StatusBar();
        this.add(statusBar, BorderLayout.PAGE_END);
        this.statusBar.setEventSource(getWwd());
    }

    /**
     * Set the globe as flat.
     * 
     * @param doMercator
     *            if <code>true</code>, mercator is used as opposed to lat/long.
     */
    public void setFlatGlobe(boolean doMercator) {
        EarthFlat globe = new EarthFlat();
        globe.setElevationModel(new ZeroElevationModel());
        wwd.getModel().setGlobe(globe);
        wwd.getView().stopMovement();
        GeographicProjection projection;
        if (doMercator) {
            projection = new ProjectionMercator();
        } else {
            projection = new ProjectionEquirectangular();
        }
        globe.setProjection(projection);
        wwd.redraw();
    }

    /**
     * Set the globe as sphere.
     */
    public void setSphereGlobe() {
        Earth globe = new Earth();
        wwd.getModel().setGlobe(globe);
        wwd.getView().stopMovement();
        wwd.redraw();
    }

    public ReferencedEnvelope getViewportBounds() {
        View view = wwd.getView();
        Position posUL = view.computePositionFromScreenPoint(0, 0);
        Position posLR = view.computePositionFromScreenPoint(getWidth(), getHeight());

        if (posLR != null && posUL != null) {
            double west = posUL.longitude.degrees;
            double north = posUL.latitude.degrees;
            double east = posLR.longitude.degrees;
            double south = posLR.latitude.degrees;

            ReferencedEnvelope env = new ReferencedEnvelope(west, east, south, north, DefaultGeographicCRS.WGS84);
            return env;
        } else {
            return null;// new ReferencedEnvelope(-180, 180, -90, 90,
                        // DefaultGeographicCRS.WGS84);
        }
    }

    /**
     * Move to a given location.
     * 
     * @param lon
     *            the longitude.
     * @param lat
     *            the latitude.
     * @param elev
     *            the eye elevation.
     * @param animate
     *            if <code>true</code>, it animates to the position.
     */
    public void goTo(double lon, double lat, Double elev, boolean animate) {
        Position eyePosition;
        if (elev == null) {
            eyePosition = NwwUtilities.toPosition(lat, lon);
        } else {
            eyePosition = NwwUtilities.toPosition(lat, lon, elev);
        }
        View view = getWwd().getView();
        if (animate) {
            if (elev == null) {
                elev = NwwUtilities.DEFAULT_ELEV;
            }
            view.goTo(eyePosition, elev);
        } else {
            view.setEyePosition(eyePosition);
        }
    }

    /**
     * Move to see a given sector.
     * 
     * @param sector the sector to go to.
     * @param animate  if <code>true</code>, it animates to the position.
     */
    public void goTo(Sector sector, boolean animate) {
        if (sector == null) {
            return;
        }
        // Create a bounding box for the specified sector in order to estimate its size in model coordinates.
        Box extent = Sector.computeBoundingBox(wwd.getModel().getGlobe(),
            wwd.getSceneController().getVerticalExaggeration(), sector);

        // Estimate the distance between the center position and the eye position that is necessary to cause the sector to
        // fill a viewport with the specified field of view. Note that we change the distance between the center and eye
        // position here, and leave the field of view constant.
        Angle fov = wwd.getView().getFieldOfView();
        double zoom = extent.getRadius() / fov.cosHalfAngle() / fov.tanHalfAngle();

        // Configure OrbitView to look at the center of the sector from our estimated distance. This causes OrbitView to
        // animate to the specified position over several seconds. To affect this change immediately use the following:

        if (animate) {
            wwd.getView().goTo(new Position(sector.getCentroid(), 0d), zoom);
        } else {
            ((OrbitView) wwd.getView()).setCenterPosition(new Position(sector.getCentroid(), 0d));
            ((OrbitView) wwd.getView()).setZoom(zoom);
        }
    }

    public WorldWindow getWwd() {
        return wwd;
    }

    public void addLayer(Layer layer) {
        getWwd().getModel().getLayers().add(layer);
    }

    public void removeLayer(Layer layer) {
        LayerList layers = getWwd().getModel().getLayers();
        layers.remove(layer);
    }

}
