package org.hortonmachine.style;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JColorChooser;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.style.LineSymbolizerWrapper;
import org.hortonmachine.gui.utils.GuiUtilities;

public class LineSymbolizerController extends LineSymbolizerView {
    private LineSymbolizerWrapper symbolizerWrapper;

    /**
    * Default constructor
     * @param mainController 
    */
    public LineSymbolizerController( LineSymbolizerWrapper symbolizerWrapper, IStyleChangeListener mainController ) {
        this.symbolizerWrapper = symbolizerWrapper;
        _applyButton.addActionListener(e -> {
            // all round until they are not an option, by default they would be butt
            symbolizerWrapper.setLineCap("round");
            symbolizerWrapper.setLineJoin("round");

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

        GuiUtilities.setupOpacitySlider(_opacityBorderSlider, OpacityValues.parse(symbolizerWrapper.getStrokeOpacity()),
                opacity -> symbolizerWrapper.setStrokeOpacity(OpacityValues.text(opacity), false));

        String borderColorStr = symbolizerWrapper.getStrokeColor();
        Color borderColor = ColorUtilities.fromHex(borderColorStr);
        GuiUtilities.colorButton(_colorBorderButton, borderColor, MainController.COLOR_IMAGE_SIZE);
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
        DashHints.setup(_dashLabel, _dashTextField, _dashOffsetLabel, _dashOffsetTextField);
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
                String tmpDashOfset = _dashOffsetTextField.getText();
                symbolizerWrapper.setDashOffset(tmpDashOfset);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });
    }


}
