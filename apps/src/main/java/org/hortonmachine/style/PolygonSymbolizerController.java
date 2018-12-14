package org.hortonmachine.style;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JColorChooser;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.style.PolygonSymbolizerWrapper;
import org.hortonmachine.gui.utils.GuiUtilities;

public class PolygonSymbolizerController extends PolygonSymbolizerView {
    private PolygonSymbolizerWrapper symbolizerWrapper;

    /**
    * Default constructor
     * @param mainController 
    */
    public PolygonSymbolizerController( PolygonSymbolizerWrapper symbolizerWrapper, MainController mainController ) {
        this.symbolizerWrapper = symbolizerWrapper;
        _applyButton.addActionListener(e -> {
            // all round until they are not an option, by default they would be butt
            symbolizerWrapper.setLineCap("round");
            symbolizerWrapper.setLineJoin("round");

            mainController.applyStyle();
        });
        init();
    }

    private void init() {
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
                symbolizerWrapper.setStrokeColor(ColorUtilities.asHex(background), false);
            }
        });

        String dash = symbolizerWrapper.getDash();
        _dashTextField.setText(dash);
        _dashTextField.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpDash = _dashTextField.getText();
                symbolizerWrapper.setDash(tmpDash);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });
        String dashOffset = symbolizerWrapper.getDashOffset();
        _dashOffsetTextField.setText(dashOffset);
        _dashOffsetTextField.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpDashOfset = _dashTextField.getText();
                symbolizerWrapper.setDashOffset(tmpDashOfset);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
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
                symbolizerWrapper.setFillColor(ColorUtilities.asHex(background), false);
            }
        });

    }

}
