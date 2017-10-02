package org.hortonmachine.nww.utils;

import org.hortonmachine.nww.layers.objects.BasicMarkerWithInfo;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwindx.hm.ToolTipController;

public class ToolTipsUtils {

    public static void addTooltipController(WorldWindow wwd) {
        new ToolTipController(wwd) {

            @Override
            public void selected(SelectEvent event) {
                if (event.getTopObject() instanceof BasicMarkerWithInfo) {
                    BasicMarkerWithInfo marker = (BasicMarkerWithInfo) event.getTopObject();
                    String info = marker.getInfo();
                    marker.setValue(AVKey.DISPLAY_NAME, info);
                }
                super.selected(event);
            }
        };
    }

}
