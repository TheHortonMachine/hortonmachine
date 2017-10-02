package org.hortonmachine.nww.layers.objects;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;

public class LayerInfo
{
    public WMSCapabilities caps;
    public AVListImpl params = new AVListImpl();

    public String getTitle()
    {
        return params.getStringValue(AVKey.DISPLAY_NAME);
    }

    public String getName()
    {
        return params.getStringValue(AVKey.LAYER_NAMES);
    }

    public String getAbstract()
    {
        return params.getStringValue(AVKey.LAYER_ABSTRACT);
    }
}