package org.hortonmachine.style;

import java.awt.Color;
import java.io.File;
import java.net.MalformedURLException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.style.PointSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.StyleUtilities;
import org.hortonmachine.gui.utils.GuiUtilities;

public class PointMarkSymbolizerController extends PointMarkSymbolizerView {
    private PointSymbolizerWrapper symbolizerWrapper;

    /**
    * Default constructor
     * @param mainController 
    */
    public PointMarkSymbolizerController( PointSymbolizerWrapper symbolizerWrapper, IStyleChangeListener mainController ) {
        this.symbolizerWrapper = symbolizerWrapper;
        _applyButton.addActionListener(e -> {
            // all round until they are not an option, by default they would be butt
//            symbolizerWrapper.setLineCap("round");
//            symbolizerWrapper.setLineJoin("round");

            mainController.applyStyle();
        });
        init();
    }

    /**
     * @param visible <code>false</code> to hide the Apply button, as when the panel is part of
     *          a panel with its own.
     */
    public void setApplyButtonVisible( boolean visible ) {
        _applyButton.setVisible(visible);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void init() {
        String[] wkmarknames = StyleUtilities.wkMarkDefs;
        _wkmarkCombo.setModel(new DefaultComboBoxModel(wkmarknames));
        String markName = symbolizerWrapper.getMarkName();
        try {
            // the path is empty, not null, if there is no graphic
            String externalGraphicPath = symbolizerWrapper.getExternalGraphicPath();
            boolean hasGraphic = externalGraphicPath != null && !externalGraphicPath.isEmpty();
            if (hasGraphic) {
                setExternalGraphics(externalGraphicPath);
            } else {
                // setting an empty path would replace the mark with an empty graphic
                _graphicPathField.setText("");
            }
            if (!hasGraphic && (markName == null || markName.isEmpty())) {
                // no mark and no graphic: show the circle
                markName = wkmarknames[2];
            }
            if (markName != null) {
                _wkmarkCombo.setSelectedItem(markName.toLowerCase());
            }
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }
        _wkmarkCombo.addActionListener(e -> {
            String wkMark = _wkmarkCombo.getSelectedItem().toString();
            symbolizerWrapper.setMarkName(wkMark);
            if (wkMark.equals("")) {
                // try graphics
                String externalGraphicPath = _graphicPathField.getText();
                setExternalGraphics(externalGraphicPath);
            }

            setMarkSize();
            setMarkRotation();
            setXOffset();
            setYOffset();
            setStrokeWidth();
            setStrokeOpacity();
            setBorderColor(null);
            setFillOpacity();
            setFillColor(null);
        });

//        _graphicPathField.setEditable(false);

        GuiUtilities.setFileBrowsingOnWidgets(_graphicPathField, _browseGraphicButton, null, () -> {
            String externalGraphicPath = _graphicPathField.getText();
            if (externalGraphicPath == null || externalGraphicPath.isBlank()) {
                // no graphic chosen: an empty one would replace the mark
                return;
            }
            try {
                symbolizerWrapper.setExternalGraphicPath(externalGraphicPath);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            }
        });

        SpinnerModel sizeModel = new SpinnerNumberModel(5, // initial value
                0, // minimum value
                100, // maximum value
                1); // step
        _markSizeSpinner.setModel(sizeModel);
        _markSizeSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getSize()));
        _markSizeSpinner.addChangeListener(e -> {
            setMarkSize();
        });

        SpinnerModel rotationModel = new SpinnerNumberModel(0, // initial value
                0, // minimum value
                360, // maximum value
                5); // step
        _rotationSpinner.setModel(rotationModel);
        _rotationSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getRotation()));
        _rotationSpinner.addChangeListener(e -> {
            setMarkRotation();
        });

        SpinnerModel offsetXModel = new SpinnerNumberModel(0, // initial value
                0, // minimum value
                100, // maximum value
                5); // step
        _offsetXSpinner.setModel(offsetXModel);
        _offsetXSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getxOffset()));
        _offsetXSpinner.addChangeListener(e -> {
            setXOffset();
        });

        SpinnerModel offsetYModel = new SpinnerNumberModel(0, // initial value
                0, // minimum value
                100, // maximum value
                5); // step
        _offsetYSpinner.setModel(offsetYModel);
        _offsetYSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getyOffset()));
        _offsetYSpinner.addChangeListener(e -> {
            setYOffset();
        });

        // BORDER and FILL
        SpinnerModel widthBorderModel = new SpinnerNumberModel(5, // initial value
                0, // minimum value
                50, // maximum value
                1); // step
        _widthSpinner.setModel(widthBorderModel);
        String strokeWidth = symbolizerWrapper.getStrokeWidth();
        if (strokeWidth == null)
            strokeWidth = "1";
        _widthSpinner.setValue((int) Double.parseDouble(strokeWidth));
        _widthSpinner.addChangeListener(e -> {
            setStrokeWidth();
        });

        GuiUtilities.setupOpacitySlider(_opacityBorderSlider, OpacityValues.parse(symbolizerWrapper.getStrokeOpacity()),
                opacity -> setStrokeOpacity());

        String borderColorStr = symbolizerWrapper.getStrokeColor();
        if (borderColorStr == null) {
            borderColorStr = "#000000";
        }
        Color borderColor = ColorUtilities.fromHex(borderColorStr);
        GuiUtilities.colorButton(_colorBorderButton, borderColor, MainController.COLOR_IMAGE_SIZE);
        _colorBorderButton.addActionListener(e -> {
            Color initialBackground = _colorBorderButton.getBackground();
            Color background = JColorChooser.showDialog(null, "Border Color Chooser", initialBackground);
            if (background != null) {
                GuiUtilities.colorButton(_colorBorderButton, background, MainController.COLOR_IMAGE_SIZE);
                setBorderColor(background);
            }
        });

        GuiUtilities.setupOpacitySlider(_opacityFillSlider, OpacityValues.parse(symbolizerWrapper.getFillOpacity()),
                opacity -> setFillOpacity());

        String fillColorStr = symbolizerWrapper.getFillColor();
        if (fillColorStr == null) {
            fillColorStr = "#FFFFFF";
        }
        Color fillColor = ColorUtilities.fromHex(fillColorStr);
        GuiUtilities.colorButton(_colorFilleButton, fillColor, MainController.COLOR_IMAGE_SIZE);
        _colorFilleButton.addActionListener(e -> {
            Color initialBackground = _colorFilleButton.getBackground();
            Color background = JColorChooser.showDialog(null, "Fill Color Chooser", initialBackground);
            if (background != null) {
                GuiUtilities.colorButton(_colorFilleButton, background, MainController.COLOR_IMAGE_SIZE);
                setFillColor(background);
            }
        });

    }

    private void setFillColor( Color background ) {
        if (background == null)
            background = _colorFilleButton.getBackground();
        symbolizerWrapper.setFillColor(ColorUtilities.asHex(background));
    }

    private void setFillOpacity() {
        symbolizerWrapper.setFillOpacity(OpacityValues.text(_opacityFillSlider.getValue() / 100.0), false);
    }

    private void setBorderColor( Color background ) {
        if (background == null)
            background = _colorBorderButton.getBackground();
        symbolizerWrapper.setStrokeColor(ColorUtilities.asHex(background));
    }

    private void setStrokeOpacity() {
        symbolizerWrapper.setStrokeOpacity(OpacityValues.text(_opacityBorderSlider.getValue() / 100.0), false);
    }

    private void setStrokeWidth() {
        symbolizerWrapper.setStrokeWidth(_widthSpinner.getValue().toString(), false);
    }

    private void setYOffset() {
        symbolizerWrapper.setOffset(symbolizerWrapper.getxOffset(), _offsetYSpinner.getValue().toString());
    }

    private void setXOffset() {
        symbolizerWrapper.setOffset(_offsetXSpinner.getValue().toString(), symbolizerWrapper.getyOffset());
    }

    private void setMarkRotation() {
        symbolizerWrapper.setRotation(_rotationSpinner.getValue().toString(), false);
    }

    private void setMarkSize() {
        symbolizerWrapper.setSize(_markSizeSpinner.getValue().toString(), false);
    }

    private void setExternalGraphics( String externalGraphicPath ) {
        try {
            if (externalGraphicPath == null) {
                _graphicPathField.setText("");
            } else {
                _graphicPathField.setText(externalGraphicPath);
            }
            symbolizerWrapper.setExternalGraphicPath(externalGraphicPath);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
