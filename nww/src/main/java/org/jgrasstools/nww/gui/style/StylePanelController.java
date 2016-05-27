package org.jgrasstools.nww.gui.style;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.SwingUtilities;

import org.jgrasstools.nww.layers.defaults.NwwVectorLayer;
import org.jgrasstools.nww.layers.defaults.NwwVectorLayer.GEOMTYPE;

import gov.nasa.worldwind.render.markers.BasicMarkerShape;

public class StylePanelController extends StylePanelView {

    private static final String[] TYPES = new String[] { BasicMarkerShape.SPHERE, BasicMarkerShape.CUBE,
            BasicMarkerShape.CONE };
    private NwwVectorLayer layer;

    private DecimalFormat oneFormat = new DecimalFormat("0.0");
    private SimpleStyle style;

    public StylePanelController(NwwVectorLayer layer) {
        this.layer = layer;
        init();
    }

    private void init() {
        style = layer.getStyle();
        GEOMTYPE geomType = layer.getType();
        switch (geomType) {
        case POINT:
            _fillPanel.setVisible(false);
            _strokelPanel.setVisible(false);

            _markerSizeText.setText(style.shapeSize + "");

            _markerTypeCombo.setModel(new DefaultComboBoxModel<String>(TYPES));
            _markerTypeCombo.setSelectedItem(style.shapeType);

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
            String[] opacities = new String[] { "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0", };
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
            style.shapeType = _markerTypeCombo.getSelectedItem().toString();
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
