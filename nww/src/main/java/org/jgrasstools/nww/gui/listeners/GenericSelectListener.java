package org.jgrasstools.nww.gui.listeners;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.jgrasstools.nww.shapes.IFeatureShape;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.opengis.feature.simple.SimpleFeature;

import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.pick.PickedObject;

public class GenericSelectListener implements SelectListener {

    private Component parent;

    public GenericSelectListener(Component parent) {
        this.parent = parent;

    }

    @Override
    public void selected(SelectEvent event) {
        PickedObject topObject = event.getTopPickedObject();

        String eventAction = event.getEventAction();
        if (eventAction.equals(SelectEvent.LEFT_CLICK)) {
            Object object = topObject.getObject();
            if (object instanceof IFeatureShape) {
                IFeatureShape featureShape = (IFeatureShape) object;
                SimpleFeature feature = featureShape.getFeature();

                LinkedHashMap<String, String> feature2AlphanumericToHashmap = NwwUtilities
                        .feature2AlphanumericToHashmap(feature);
                StringBuilder sb = new StringBuilder();
                for (Entry<String, String> entry : feature2AlphanumericToHashmap.entrySet()) {
                    sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                String msg = sb.toString();
                
                if (msg.trim().length()==0) {
                    msg = "No additional info available";
                }
                JOptionPane.showMessageDialog(parent, msg);

                event.consume();
            }
        }
    }

}
