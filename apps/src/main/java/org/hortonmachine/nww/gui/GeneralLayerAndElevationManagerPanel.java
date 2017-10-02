/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import gov.nasa.worldwind.WorldWindow;

/**
 * Combines the layer manager and elevation model manager in a single frame.
 *
 * @author tag
 * @version $Id: LayerAndElevationManagerPanel.java 2109 2014-06-30 16:52:38Z tgaskins $
 */
public class GeneralLayerAndElevationManagerPanel extends JPanel
{
    protected LayerManagerPanel layerManagerPanel;
    protected ElevationModelManagerPanel elevationModelManagerPanel;

    public GeneralLayerAndElevationManagerPanel(WorldWindow wwd)
    {
        super(new BorderLayout(10, 10));

        this.add(this.layerManagerPanel = new LayerManagerPanel(wwd), BorderLayout.CENTER);

        this.add(this.elevationModelManagerPanel = new ElevationModelManagerPanel(wwd), BorderLayout.SOUTH);
    }

    public void updateLayers(WorldWindow wwd)
    {
        this.layerManagerPanel.update(wwd);
    }

    public void updateElevations(WorldWindow wwd)
    {
        this.elevationModelManagerPanel.update(wwd);
    }

    public void update(WorldWindow wwd)
    {
        this.updateLayers(wwd);
        this.updateElevations(wwd);
    }
}
