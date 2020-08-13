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
    public PointMarkSymbolizerController( PointSymbolizerWrapper symbolizerWrapper, MainController mainController ) {
        this.symbolizerWrapper = symbolizerWrapper;
        _applyButton.addActionListener(e -> {
            // all round until they are not an option, by default they would be butt
//            symbolizerWrapper.setLineCap("round");
//            symbolizerWrapper.setLineJoin("round");

            mainController.applyStyle();
        });
        init();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void init() {
        String[] wkmarknames = StyleUtilities.wkMarkDefs;
        _wkmarkCombo.setModel(new DefaultComboBoxModel(wkmarknames));
        String markName = symbolizerWrapper.getMarkName();
        try {
            String externalGraphicPath = symbolizerWrapper.getExternalGraphicPath();
            setExternalGraphics(externalGraphicPath);
            if (externalGraphicPath == null) {
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

        SpinnerModel opacityBorderModel = new SpinnerNumberModel(255, // initial value
                0, // minimum value
                255, // maximum value
                5); // step
        _opacityBorderSpinner.setModel(opacityBorderModel);
        String strokeOpacity = symbolizerWrapper.getStrokeOpacity();
        if (strokeOpacity == null) {
            strokeOpacity = "1";
        }
        double borderOpacity = Double.parseDouble(strokeOpacity);
        borderOpacity *= 255;
        _opacityBorderSpinner.setValue((int) borderOpacity);
        _opacityBorderSpinner.addChangeListener(e -> {
            setStrokeOpacity();
        });

        String borderColorStr = symbolizerWrapper.getStrokeColor();
        if (borderColorStr == null) {
            borderColorStr = "#000000";
        }
        Color borderColor = ColorUtilities.fromHex(borderColorStr);
        GuiUtilities.colorButton(_colorBorderButton, borderColor, MainController.COLOR_IMAGE_SIZE);
        _colorBorderButton.setBackground(borderColor);
        _colorBorderButton.addActionListener(e -> {
            Color initialBackground = _colorBorderButton.getBackground();
            Color background = JColorChooser.showDialog(null, "Border Color Chooser", initialBackground);
            if (background != null) {
                GuiUtilities.colorButton(_colorBorderButton, background, MainController.COLOR_IMAGE_SIZE);
                setBorderColor(background);
            }
        });

        SpinnerModel opacityFillModel = new SpinnerNumberModel(255, // initial value
                0, // minimum value
                255, // maximum value
                5); // step
        _opacityFillSpinner.setModel(opacityFillModel);
        String fillOpacityStr = symbolizerWrapper.getFillOpacity();
        if (fillOpacityStr == null) {
            fillOpacityStr = "1";
        }
        double fillOpacity = Double.parseDouble(fillOpacityStr);
        fillOpacity *= 255;
        _opacityFillSpinner.setValue((int) fillOpacity);
        _opacityFillSpinner.addChangeListener(e -> {
            setFillOpacity();
        });

        String fillColorStr = symbolizerWrapper.getFillColor();
        if (fillColorStr == null) {
            fillColorStr = "#FFFFFF";
        }
        Color fillColor = ColorUtilities.fromHex(fillColorStr);
        GuiUtilities.colorButton(_colorFilleButton, fillColor, MainController.COLOR_IMAGE_SIZE);
        _colorFilleButton.setBackground(fillColor);
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
        int intValue = ((Number) _opacityFillSpinner.getValue()).intValue();
        float opacity = intValue / 255f;
        symbolizerWrapper.setFillOpacity(opacity + "", false);
    }

    private void setBorderColor( Color background ) {
        if (background == null)
            background = _colorBorderButton.getBackground();
        symbolizerWrapper.setStrokeColor(ColorUtilities.asHex(background));
    }

    private void setStrokeOpacity() {
        int intValue = ((Number) _opacityBorderSpinner.getValue()).intValue();
        float opacity = intValue / 255f;
        symbolizerWrapper.setStrokeOpacity(opacity + "", false);
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
