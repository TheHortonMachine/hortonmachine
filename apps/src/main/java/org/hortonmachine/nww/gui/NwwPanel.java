/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.nww.layers.defaults.raster.OSMMapnikLayer;
import org.hortonmachine.nww.utils.NwwUtilities;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
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
import gov.nasa.worldwind.terrain.ZeroElevationModel;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwind.view.orbit.OrbitViewLimits;

/**
 * The main NWW panel.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("serial")
public class NwwPanel extends JPanel {
    private static final Logger logger = Logger.INSTANCE;

    private WorldWindow wwd;
    protected StatusBar statusBar;

    private double lastElevation = Double.NaN;

    public static Component createNwwPanel( boolean useWwGlCanvas, boolean withStatusBar, boolean removeDefaultLayers ) {
        Component component = null;
        try {
            component = new NwwPanel(useWwGlCanvas, withStatusBar, removeDefaultLayers);
        } catch (UnsatisfiedLinkError ule) {
            logger.insertError("NwwPanel", "error", ule);
            String msg = "<html><b><font color=red size=+1>";
            msg += "<p>An error occurred while loading the native NWW libraries,</p>";
            msg += "<p>check your installation.</p><p></p>";
            msg += "<i><p>The error is: </p><p>" + ule.getMessage();
            msg += "</p></i></font></b>";
            JLabel errorLabel = new JLabel(msg);
            Border paddingBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);
            errorLabel.setBorder(paddingBorder);
            component = errorLabel;
        } catch (Exception e) {
            e.printStackTrace();
            logger.insertError("NwwPanel", "error", e);
        }

        return component;
    }

    public static Component createNwwPanel( boolean useWwGlCanvas ) {
        return createNwwPanel(useWwGlCanvas, true, true);
    }

    protected NwwPanel( boolean useWwGlCanvas, boolean withStatusBar, boolean removeDefaultLayers ) {
        super(new BorderLayout());

        // Configuration.setValue(AVKey.INITIAL_LATITUDE, gpsLogShps[0].y);
        // Configuration.setValue(AVKey.INITIAL_LONGITUDE, gpsLogShps[0].x);
        // Configuration.setValue(AVKey.INITIAL_ALTITUDE, 1000);
        // Configuration.setValue(AVKey.INITIAL_PITCH, 45);

        long t1 = System.currentTimeMillis();
        if (useWwGlCanvas) {
            logger.insertDebug("NwwPanel", "Create GLCanvas");
            wwd = new WorldWindowGLCanvas();
        } else {
            logger.insertDebug("NwwPanel", "Create GLJPanel");
            wwd = new WorldWindowGLJPanel();
        }
        // ((Component) wwd).setPreferredSize(new Dimension(500, 500));
        long t2 = System.currentTimeMillis();
        logger.insertDebug("NwwPanel", "Create Canvas - DONE " + (t2 - t1) / 1000);

        logger.insertDebug("NwwPanel", "Create Model");
        Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        wwd.setModel(model);
        long t3 = System.currentTimeMillis();
        logger.insertDebug("NwwPanel", "Create Model - DONE " + (t3 - t2) / 1000);

        if (removeDefaultLayers) {
            logger.insertDebug("NwwPanel", "Remove and add layers");
            LayerList layers = model.getLayers();
            List<Layer> addBack = new ArrayList<>();
            Iterator<Layer> layerIterator = layers.iterator();
            List<String> namesToKeep = NwwUtilities.LAYERS_TO_KEEP_FROM_ORIGNALNWW;
            while( layerIterator.hasNext() ) {
                Layer layer = layerIterator.next();
                if (namesToKeep.contains(layer.getName())) {
                    addBack.add(layer);
                }
            }
            layers.clear();
            layers.addAll(addBack);
            long t4 = System.currentTimeMillis();
            logger.insertDebug("NwwPanel", "Remove and add layers - DONE " + (t4 - t3) / 1000);
        }
        this.add((Component) wwd, BorderLayout.CENTER);

        if (withStatusBar) {
            this.statusBar = new StatusBar();
            this.add(statusBar, BorderLayout.PAGE_END);
            this.statusBar.setEventSource(getWwd());
        }
    }

    public ViewControlsLayer addViewControls( double scale, boolean showZoomControls, boolean showPanControls,
            boolean showheadingControls, boolean showPitchControls, boolean showVerticalExaggerationControls ) {
        ViewControlsLayer viewControlsLayer = new ViewControlsLayer();

        viewControlsLayer.setScale(scale);
        viewControlsLayer.setShowZoomControls(showZoomControls);
        viewControlsLayer.setShowPanControls(showPanControls);
        viewControlsLayer.setShowHeadingControls(showheadingControls);
        viewControlsLayer.setShowPitchControls(showPitchControls);
        viewControlsLayer.setShowVeControls(showVerticalExaggerationControls);

        addLayer(viewControlsLayer);
        getWwd().addSelectListener(new ViewControlsSelectListener(getWwd(), viewControlsLayer));
        return viewControlsLayer;
    }

    public ViewControlsLayer addViewControls() {
        return addViewControls(2, true, false, true, false, false);
    }

    public void addOsmLayer() {
        addLayer(new OSMMapnikLayer());
    }

    public void setZoomLimits( double minZoom, double maxZoom ) {
        View view = getWwd().getView();
        if (view != null && view instanceof OrbitView) {
            OrbitView oView = (OrbitView) view;
            OrbitViewLimits orbitViewLimits = oView.getOrbitViewLimits();
            orbitViewLimits.setZoomLimits(minZoom, maxZoom);
        }
    }

    public void setPitchLimits( Angle minAngle, Angle maxangle ) {
        View view = getWwd().getView();
        if (view != null && view instanceof OrbitView) {
            OrbitView oView = (OrbitView) view;
            OrbitViewLimits orbitViewLimits = oView.getOrbitViewLimits();
            orbitViewLimits.setPitchLimits(minAngle, maxangle);
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
     * @param azimuth
     *            if supplied, the map is rotated to follow that angle.
     * @param animate
     *            if <code>true</code>, it animates to the position.
     */
    public synchronized Position goTo( Double lon, Double lat, Double elev, Double azimuth, boolean animate ) {
        View view = getWwd().getView();
        view.stopAnimations();
        view.stopMovement();

        Position eyePosition;
        if (lon == null || lat == null) {
            Position currentEyePosition = wwd.getView().getCurrentEyePosition();
            if (currentEyePosition != null) {
                lat = currentEyePosition.latitude.degrees;
                lon = currentEyePosition.longitude.degrees;
            } else {
                return null;
            }
        }

        if (elev == null) {
            // use the current
            elev = wwd.getView().getCurrentEyePosition().getAltitude();
        }
        if (Double.isNaN(elev)) {
            if (!Double.isNaN(lastElevation)) {
                elev = lastElevation;
            } else {
                elev = NwwUtilities.DEFAULT_ELEV;
            }
        }
        eyePosition = NwwUtilities.toPosition(lat, lon, elev);
        if (animate) {
            view.goTo(eyePosition, elev);
        } else {
            view.setEyePosition(eyePosition);
        }
        if (azimuth != null) {
            Angle heading = Angle.fromDegrees(azimuth);
            view.setHeading(heading);
        }
        lastElevation = elev;
        return eyePosition;
    }

    /**
     * Move to see a given sector.
     * 
     * @param sector
     *            the sector to go to.
     * @param animate
     *            if <code>true</code>, it animates to the position.
     */
    public void goTo( Sector sector, boolean animate ) {
        View view = getWwd().getView();
        view.stopAnimations();
        view.stopMovement();
        if (sector == null) {
            return;
        }
        // Create a bounding box for the specified sector in order to estimate
        // its size in model coordinates.
        Box extent = Sector.computeBoundingBox(getWwd().getModel().getGlobe(),
                getWwd().getSceneController().getVerticalExaggeration(), sector);

        // Estimate the distance between the center position and the eye
        // position that is necessary to cause the sector to
        // fill a viewport with the specified field of view. Note that we change
        // the distance between the center and eye
        // position here, and leave the field of view constant.
        Angle fov = view.getFieldOfView();
        double zoom = extent.getRadius() / fov.cosHalfAngle() / fov.tanHalfAngle();

        // Configure OrbitView to look at the center of the sector from our
        // estimated distance. This causes OrbitView to
        // animate to the specified position over several seconds. To affect
        // this change immediately use the following:

        if (animate) {
            view.goTo(new Position(sector.getCentroid(), 0d), zoom);
        } else {
            ((OrbitView) wwd.getView()).setCenterPosition(new Position(sector.getCentroid(), 0d));
            ((OrbitView) wwd.getView()).setZoom(zoom);
        }
    }

    /**
     * Set the globe as flat.
     * 
     * @param doMercator
     *            if <code>true</code>, mercator is used as opposed to lat/long.
     */
    public void setFlatGlobe( boolean doMercator ) {
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

    /**
     * Set the globe as flat sphere.
     */
    public void setFlatSphereGlobe() {
        Earth globe = new Earth();
        globe.setElevationModel(new ZeroElevationModel());
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

    public WorldWindow getWwd() {
        return wwd;
    }

    public void addLayer( Layer layer ) {
        getWwd().getModel().getLayers().add(layer);
    }

    public void removeLayer( Layer layer ) {
        LayerList layers = getWwd().getModel().getLayers();
        layers.remove(layer);
    }

    public void shutdown() {
        if (wwd != null) {
            wwd.shutdown();
        }
    }

    // TODO check these old zoom tos
    // public void zoomTo(Sector sector, boolean animate) {
    // if (sector != null) {
    // double sectorWidth = sector.getDeltaLonDegrees();
    // LatLon centroid = sector.getCentroid();
    //
    // zoomTo(sectorWidth, centroid, animate);
    // }
    // }
    //
    // public void zoomTo(ReferencedEnvelope env, boolean animate) {
    // double sectorWidth = env.getWidth();
    // Coordinate centre = env.centre();
    // LatLon centroid = NwwUtilities.toLatLon(centre.y, centre.x);
    //
    // zoomTo(sectorWidth, centroid, animate);
    // }
    //
    // private void zoomTo(double width, LatLon centroid, boolean animate) {
    // View view = getWwd().getView();
    // double altitude = view.getCurrentEyePosition().getAltitude();
    // ReferencedEnvelope viewportBounds = getViewportBounds();
    // double newAltitude;
    // if (viewportBounds != null) {
    // double viewWidth = viewportBounds.getWidth();
    // newAltitude = width * altitude / viewWidth;
    // } else {
    // newAltitude = altitude / 3;
    // }
    // goTo(centroid.longitude.degrees, centroid.latitude.degrees, newAltitude,
    // null, animate);
    // }

}
