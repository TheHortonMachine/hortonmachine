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

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Sector;

/**
 * Box selector with listeners.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SectorByBoxSelector extends SectorSelector {

    public static interface IBoxSelectionListener {

        void onSelectionFinished(Sector selectedSector);
    }

    private List<IBoxSelectionListener> listeners = new ArrayList<>();

    private boolean wasDragged = false;

    public SectorByBoxSelector(WorldWindow worldWindow) {
        super(worldWindow);

        setInteriorColor(Color.BLUE);
        setInteriorOpacity(0.3f);
        setBorderColor(Color.BLUE);
        setBorderOpacity(1.0);
        setBorderWidth(1.0);
    }

    public void addListener(IBoxSelectionListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(IBoxSelectionListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    public void mousePressed(MouseEvent mouseEvent) {
        super.mousePressed(mouseEvent);
        wasDragged = false;
    }

    public void mouseDragged(MouseEvent mouseEvent) {
        super.mouseDragged(mouseEvent);
        wasDragged = true;
    }

    public void mouseReleased(MouseEvent mouseEvent) {
        super.mouseReleased(mouseEvent);

        if (wasDragged) {
            Sector sector = getSector();
            for (IBoxSelectionListener iBoxSelectionListener : listeners) {
                iBoxSelectionListener.onSelectionFinished(sector);
            }
        }
    }

}
