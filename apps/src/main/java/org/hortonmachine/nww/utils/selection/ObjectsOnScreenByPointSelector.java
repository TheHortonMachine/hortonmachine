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

import org.locationtech.jts.geom.Point;

import gov.nasa.worldwind.WorldWindow;

/**
 * Box selector with listeners.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ObjectsOnScreenByPointSelector extends ScreenSelector {

    public static interface IPointScreenSelectionListener {

        void onSelectionFinished(Point selectedPoint, List<?> selectedObjs);
    }

    private List<IPointScreenSelectionListener> listeners = new ArrayList<>();

    public ObjectsOnScreenByPointSelector(WorldWindow worldWindow) {
        super(worldWindow);

        // setInteriorColor(Color.BLUE);
        // setInteriorOpacity(0.3f);
        // setBorderColor(Color.BLUE);
        // setBorderOpacity(1.0);
        // setBorderWidth(1.0);
    }

    public void addListener(IPointScreenSelectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(IPointScreenSelectionListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }
    
    @Override
    public void mousePressed(MouseEvent mouseEvent) {
    }
    
    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        int endX = mouseEvent.getX();
        int endY = mouseEvent.getY();

        List<?> selectedObjs = getSelectedObjects();
        Point screenPoint = NwwUtilities.getScreenPoint(wwd, endX, endY);
        for (IPointScreenSelectionListener iBoxSelectionListener : listeners) {
            iBoxSelectionListener.onSelectionFinished(screenPoint, selectedObjs);
        }
        super.mouseReleased(mouseEvent);
    }

}
