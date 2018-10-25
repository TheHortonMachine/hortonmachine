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
package org.hortonmachine.nww.utils.selection;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.nww.utils.NwwUtilities;

import org.locationtech.jts.geom.Geometry;

import gov.nasa.worldwind.WorldWindow;

/**
 * Box selector with listeners.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ObjectsOnScreenByBoxSelector extends ScreenSelector {

    public static interface IBoxScreenSelectionListener {

        void onSelectionFinished(Geometry selectedLatLongPolygon, List<?> selectedObjs);
    }

    private List<IBoxScreenSelectionListener> listeners = new ArrayList<>();

    private boolean wasDragged = false;

    private int startX;

    private int startY;

    public ObjectsOnScreenByBoxSelector(WorldWindow worldWindow) {
        super(worldWindow);

        // setInteriorColor(Color.BLUE);
        // setInteriorOpacity(0.3f);
        // setBorderColor(Color.BLUE);
        // setBorderOpacity(1.0);
        // setBorderWidth(1.0);
    }

    public void addListener(IBoxScreenSelectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(IBoxScreenSelectionListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    public void mousePressed(MouseEvent mouseEvent) {
        super.mousePressed(mouseEvent);
        wasDragged = false;
        startX = mouseEvent.getX();
        startY = mouseEvent.getY();
    }

    public void mouseDragged(MouseEvent mouseEvent) {
        super.mouseDragged(mouseEvent);
        wasDragged = true;
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        super.mouseReleased(mouseEvent);
        int endX = mouseEvent.getX();
        int endY = mouseEvent.getY();

        if (wasDragged) {
            List<?> selectedObjs = getSelectedObjects();
            Geometry screenPointsPolygon = NwwUtilities.getScreenPointsPolygon(wwd, startX, startY, endX, endY);
            for (IBoxScreenSelectionListener iBoxSelectionListener : listeners) {
                iBoxSelectionListener.onSelectionFinished(screenPointsPolygon, selectedObjs);
            }
        }
    }

}
