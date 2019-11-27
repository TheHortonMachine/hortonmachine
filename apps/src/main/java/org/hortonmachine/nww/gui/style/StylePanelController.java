package org.hortonmachine.nww.gui.style;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.SwingUtilities;

import org.hortonmachine.nww.layers.defaults.NwwVectorLayer;
import org.hortonmachine.nww.layers.defaults.NwwVectorLayer.GEOMTYPE;
import org.hortonmachine.style.SimpleStyle;

import gov.nasa.worldwind.render.markers.BasicMarkerShape;

public class StylePanelController extends StylePanelView {

    private static final String[] TYPES = new String[] { "SPHERE", "CUBE", "CONE" };

    private static String[] opacities =
        new String[] { "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0", };

    private NwwVectorLayer layer;

    private DecimalFormat oneFormat = new DecimalFormat("0.0");
    private SimpleStyle style;

    private HashMap<String, String> shapeMap;

    public StylePanelController(NwwVectorLayer layer) {
        this.layer = layer;

        init();
    }

    private void init() {
        shapeMap = new HashMap<>();
        shapeMap.put(TYPES[0], BasicMarkerShape.SPHERE);
        shapeMap.put(TYPES[1], BasicMarkerShape.CUBE);
        shapeMap.put(TYPES[2], BasicMarkerShape.CONE);
        HashMap<String, String> inverseShapeMap = new HashMap<>();
        inverseShapeMap.put(BasicMarkerShape.SPHERE, TYPES[0]);
        inverseShapeMap.put(BasicMarkerShape.CUBE, TYPES[1]);
        inverseShapeMap.put(BasicMarkerShape.CONE, TYPES[2]);

        style = layer.getStyle();
        GEOMTYPE geomType = layer.getType();
        switch (geomType) {
        case POINT:
            //            _fillPanel.setVisible(false);
            _strokelPanel.setVisible(false);

            _markerSizeText.setText(style.shapeSize + "");

            _markerTypeCombo.setModel(new DefaultComboBoxModel<String>(TYPES));
            _markerTypeCombo.setSelectedItem(inverseShapeMap.get(style.shapeType));

            _fillColorButton.setBackground(style.fillColor);
            _fillColorButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Color newColor = JColorChooser.showDialog(null, "Choose a color", style.fillColor);
                    if (newColor != null) {
                        style.fillColor = newColor;
                        _fillColorButton.setBackground(style.fillColor);
                    }
                }
            });
            _fillOpacityCombo.setModel(new DefaultComboBoxModel<String>(opacities));
            _fillOpacityCombo.setSelectedItem(oneFormat.format(style.fillOpacity));

            break;
        case LINE:
            _fillPanel.setVisible(false);
            _markerPanel.setVisible(false);

            _strokeColorButton.setBackground(style.strokeColor);
            _strokeColorButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Color newColor = JColorChooser.showDialog(null, "Choose a color", style.strokeColor);
                    if (newColor != null) {
                        style.strokeColor = newColor;
                        _strokeColorButton.setBackground(style.strokeColor);
                    }
                }
            });

            _strokeWidthText.setText(style.strokeWidth + "");

            break;
        case POLYGON:
            _markerPanel.setVisible(false);

            _strokeColorButton.setBackground(style.strokeColor);
            _strokeColorButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Color newColor = JColorChooser.showDialog(null, "Choose a color", style.strokeColor);
                    if (newColor != null) {
                        style.strokeColor = newColor;
                        _strokeColorButton.setBackground(style.strokeColor);
                    }
                }
            });
            _strokeWidthText.setText(style.strokeWidth + "");

            _fillColorButton.setBackground(style.fillColor);
            _fillColorButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Color newColor = JColorChooser.showDialog(null, "Choose a color", style.fillColor);
                    if (newColor != null) {
                        style.fillColor = newColor;
                        _fillColorButton.setBackground(style.fillColor);
                    }
                }
            });
            _fillOpacityCombo.setModel(new DefaultComboBoxModel<String>(opacities));
            _fillOpacityCombo.setSelectedItem(oneFormat.format(style.fillOpacity));

            break;

        default:
            break;
        }

        _okButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                applyStyle();
                Window w = SwingUtilities.getWindowAncestor(StylePanelController.this);
                w.setVisible(false);
            }
        });
        _cancelButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Window w = SwingUtilities.getWindowAncestor(StylePanelController.this);
                w.setVisible(false);
            }
        });
    }

    protected void applyStyle() {
        GEOMTYPE geomType = layer.getType();
        switch (geomType) {
        case POINT:
            String sizeText = _markerSizeText.getText();
            try {
                style.shapeSize = Double.parseDouble(sizeText);
            } catch (NumberFormatException e1) {
                e1.printStackTrace();
            }
            style.shapeType = shapeMap.get(_markerTypeCombo.getSelectedItem().toString());
            try {
                String opacString = _fillOpacityCombo.getSelectedItem().toString();
                style.fillOpacity = Double.parseDouble(opacString);
            } catch (NumberFormatException e1) {
                e1.printStackTrace();
            }
            break;
        case LINE:
            String widthText = _strokeWidthText.getText();
            try {
                style.strokeWidth = Double.parseDouble(widthText);
            } catch (NumberFormatException e1) {
                e1.printStackTrace();
            }
            break;
        case POLYGON:
            widthText = _strokeWidthText.getText();
            try {
                style.strokeWidth = Double.parseDouble(widthText);
            } catch (NumberFormatException e1) {
                e1.printStackTrace();
            }
            try {
                String opacString = _fillOpacityCombo.getSelectedItem().toString();
                style.fillOpacity = Double.parseDouble(opacString);
            } catch (NumberFormatException e1) {
                e1.printStackTrace();
            }

            break;

        default:
            break;
        }

        layer.setStyle(style);
    }

}
