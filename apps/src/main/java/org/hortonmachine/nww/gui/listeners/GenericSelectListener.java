package org.hortonmachine.nww.gui.listeners;

import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JOptionPane;

import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.nww.shapes.FeatureStoreInfo;
import org.hortonmachine.nww.shapes.IFeatureShape;
import org.hortonmachine.nww.shapes.IInfoShape;
import org.hortonmachine.nww.utils.NwwUtilities;
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
            String msg = null;
            if (object instanceof IFeatureShape) {
                IFeatureShape featureShape = (IFeatureShape) object;
                SimpleFeature feature = featureShape.getFeature();
                FeatureStoreInfo featureStoreInfo = featureShape.getFeatureStoreInfo();

                LinkedHashMap<String, String> feature2AlphanumericToHashmap = NwwUtilities
                        .feature2AlphanumericToHashmap(feature);
                if (featureStoreInfo.getFeatureStore() == null) {
                    StringBuilder sb = new StringBuilder();
                    for (Entry<String, String> entry : feature2AlphanumericToHashmap.entrySet()) {
                        sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    }
                    msg = sb.toString();
                } else {
                    int size = feature2AlphanumericToHashmap.size();
                    String[] fieldNames = new String[size];
                    String[] values = new String[size];
                    Set<Entry<String, String>> entrySet = feature2AlphanumericToHashmap.entrySet();
                    int count = 0;
                    for (Entry<String, String> entry : entrySet) {
                        fieldNames[count] = entry.getKey();
                        String value = entry.getValue();
                        if (value == null) {
                            value = "";
                        }
                        values[count] = value;
                        count++;
                    }

                    String[] editedValues = GuiUtilities.showMultiInputDialog(parent, "Edit feature", fieldNames,
                            values, featureStoreInfo.getField2ValuesMap());
                    if (editedValues != null) {
                        SimpleFeature modifiedFeature = featureShape.modifyFeatureAttribute(fieldNames, editedValues);
                        if (modifiedFeature != null) {
                            featureShape.setFeature(modifiedFeature);
                        }
                    }
                }
            }
            if (object instanceof IInfoShape) {
                IInfoShape infoShape = (IInfoShape) object;
                msg = infoShape.getInfo();
            }
            if (msg != null) {
                if (msg.trim().length() == 0) {
                    msg = "No additional info available";
                }
                JOptionPane.showMessageDialog(parent, msg);
                event.consume();
            }
        }
    }

}
