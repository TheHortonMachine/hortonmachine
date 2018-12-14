package org.hortonmachine.style;

import java.awt.Color;

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
        if (markName == null) {
            markName = wkmarknames[2];
        }
        _wkmarkCombo.setSelectedItem(markName);
        _wkmarkCombo.addActionListener(e -> {
            String wkMark = _wkmarkCombo.getSelectedItem().toString();
            symbolizerWrapper.setMarkName(wkMark);
        });

        SpinnerModel sizeModel = new SpinnerNumberModel(5, // initial value
                0, // minimum value
                100, // maximum value
                1); // step
        _markSizeSpinner.setModel(sizeModel);
        _markSizeSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getSize()));
        _markSizeSpinner.addChangeListener(e -> {
            symbolizerWrapper.setSize(_markSizeSpinner.getValue().toString(), false);
        });

        SpinnerModel rotationModel = new SpinnerNumberModel(0, // initial value
                0, // minimum value
                360, // maximum value
                5); // step
        _rotationSpinner.setModel(rotationModel);
        _rotationSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getRotation()));
        _rotationSpinner.addChangeListener(e -> {
            symbolizerWrapper.setRotation(_rotationSpinner.getValue().toString(), false);
        });

        SpinnerModel offsetXModel = new SpinnerNumberModel(0, // initial value
                0, // minimum value
                100, // maximum value
                5); // step
        _offsetXSpinner.setModel(offsetXModel);
        _offsetXSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getxOffset()));
        _offsetXSpinner.addChangeListener(e -> {
            symbolizerWrapper.setOffset(_offsetXSpinner.getValue().toString(), symbolizerWrapper.getyOffset());
        });

        SpinnerModel offsetYModel = new SpinnerNumberModel(0, // initial value
                0, // minimum value
                100, // maximum value
                5); // step
        _offsetYSpinner.setModel(offsetYModel);
        _offsetYSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getyOffset()));
        _offsetYSpinner.addChangeListener(e -> {
            symbolizerWrapper.setOffset(symbolizerWrapper.getxOffset(), _offsetYSpinner.getValue().toString());
        });

        // BORDER and FILL
        SpinnerModel widthBorderModel = new SpinnerNumberModel(5, // initial value
                0, // minimum value
                50, // maximum value
                1); // step
        _widthSpinner.setModel(widthBorderModel);
        _widthSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getStrokeWidth()));
        _widthSpinner.addChangeListener(e -> {
            symbolizerWrapper.setStrokeWidth(_widthSpinner.getValue().toString(), false);
        });

        SpinnerModel opacityBorderModel = new SpinnerNumberModel(255, // initial value
                0, // minimum value
                255, // maximum value
                5); // step
        _opacityBorderSpinner.setModel(opacityBorderModel);
        double borderOpacity = Double.parseDouble(symbolizerWrapper.getStrokeOpacity());
        borderOpacity *= 255;
        _opacityBorderSpinner.setValue((int) borderOpacity);
        _opacityBorderSpinner.addChangeListener(e -> {
            int intValue = ((Number) _opacityBorderSpinner.getValue()).intValue();
            float opacity = intValue / 255f;
            symbolizerWrapper.setStrokeOpacity(opacity + "", false);
        });

        String borderColorStr = symbolizerWrapper.getStrokeColor();
        Color borderColor = ColorUtilities.fromHex(borderColorStr);
        GuiUtilities.colorButton(_colorBorderButton, borderColor, MainController.COLOR_IMAGE_SIZE);
        _colorBorderButton.setBackground(borderColor);
        _colorBorderButton.addActionListener(e -> {
            Color initialBackground = _colorBorderButton.getBackground();
            Color background = JColorChooser.showDialog(null, "Border Color Chooser", initialBackground);
            if (background != null) {
                GuiUtilities.colorButton(_colorBorderButton, background, MainController.COLOR_IMAGE_SIZE);
                symbolizerWrapper.setStrokeColor(ColorUtilities.asHex(background));
            }
        });

        SpinnerModel opacityFillModel = new SpinnerNumberModel(255, // initial value
                0, // minimum value
                255, // maximum value
                5); // step
        _opacityFillSpinner.setModel(opacityFillModel);
        double fillOpacity = Double.parseDouble(symbolizerWrapper.getFillOpacity());
        fillOpacity *= 255;
        _opacityFillSpinner.setValue((int) fillOpacity);
        _opacityFillSpinner.addChangeListener(e -> {
            int intValue = ((Number) _opacityFillSpinner.getValue()).intValue();
            float opacity = intValue / 255f;
            symbolizerWrapper.setFillOpacity(opacity + "", false);
        });

        String fillColorStr = symbolizerWrapper.getFillColor();
        Color fillColor = ColorUtilities.fromHex(fillColorStr);
        GuiUtilities.colorButton(_colorFilleButton, fillColor, MainController.COLOR_IMAGE_SIZE);
        _colorFilleButton.setBackground(fillColor);
        _colorFilleButton.addActionListener(e -> {
            Color initialBackground = _colorFilleButton.getBackground();
            Color background = JColorChooser.showDialog(null, "Fill Color Chooser", initialBackground);
            if (background != null) {
                GuiUtilities.colorButton(_colorFilleButton, background, MainController.COLOR_IMAGE_SIZE);
                symbolizerWrapper.setFillColor(ColorUtilities.asHex(background));
            }
        });

    }

}
