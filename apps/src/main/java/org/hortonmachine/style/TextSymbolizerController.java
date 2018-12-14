package org.hortonmachine.style;

import java.awt.Color;
import java.awt.Font;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.style.PointSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.StyleUtilities;
import org.hortonmachine.gears.utils.style.TextSymbolizerWrapper;
import org.hortonmachine.gui.utils.GuiUtilities;
import gov.nasa.worldwind.layers.Earth.LandsatI3WMSLayer;

public class TextSymbolizerController extends TextSymbolizerView {
    private TextSymbolizerWrapper symbolizerWrapper;
    private String[] fields;

    /**
    * Default constructor
     * @param mainController 
    */
    public TextSymbolizerController( TextSymbolizerWrapper symbolizerWrapper, String[] fields, MainController mainController ) {
        this.symbolizerWrapper = symbolizerWrapper;
        this.fields = fields;
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
        _labelCombo.setModel(new DefaultComboBoxModel(fields));
        String fieldName = symbolizerWrapper.getLabelName();
        if (fieldName != null)
            _labelCombo.setSelectedItem(fieldName);
        _labelCombo.addActionListener(e -> {
            String field = _labelCombo.getSelectedItem().toString();
            symbolizerWrapper.setLabelName(field, true);
        });

        SpinnerModel opacityModel = new SpinnerNumberModel(255, // initial value
                0, // minimum value
                255, // maximum value
                5); // step
        _opacitySpinner.setModel(opacityModel);
        double opacity = Double.parseDouble(symbolizerWrapper.getOpacity());
        opacity *= 255;
        _opacitySpinner.setValue((int) opacity);
        _opacitySpinner.addChangeListener(e -> {
            int intValue = ((Number) _opacitySpinner.getValue()).intValue();
            float newOpacity = intValue / 255f;
            symbolizerWrapper.setOpacity(newOpacity + "", false);
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

        // TODO font
        String fontFamily = symbolizerWrapper.getFontFamily();
        String fontSize = symbolizerWrapper.getFontSize();
        String fontStyle = symbolizerWrapper.getFontStyle();
        String fontWeight = symbolizerWrapper.getFontWeight();
        Font font = new Font(fontFamily, Font.BOLD, (int) Double.parseDouble(fontSize));
        _fontButton.addActionListener(e -> {
            JFontChooser chooser = new JFontChooser();
            Font font2 = chooser.showDialog(this, "Choose a font");

            JOptionPane.showMessageDialog(this,
                    font2 == null
                            ? "You canceled the dialog."
                            : "You have selected " + font2.getName() + ", " + font2.getSize() + (font2.isBold() ? ", Bold" : "")
                                    + (font2.isItalic() ? ", Italic" : ""));
        });

        String fontColorStr = symbolizerWrapper.getColor();
        Color fontColor = ColorUtilities.fromHex(fontColorStr);
        GuiUtilities.colorButton(_colorFontButton, fontColor, MainController.COLOR_IMAGE_SIZE);
        _colorFontButton.setBackground(fontColor);
        _colorFontButton.addActionListener(e -> {
            Color initialBackground = _colorFontButton.getBackground();
            Color background = JColorChooser.showDialog(null, "Font Color Chooser", initialBackground);
            if (background != null) {
                GuiUtilities.colorButton(_colorFontButton, background, MainController.COLOR_IMAGE_SIZE);
                symbolizerWrapper.setColor(ColorUtilities.asHex(background));
            }
        });

        SpinnerModel haloSizeModel = new SpinnerNumberModel(1, // initial value
                0, // minimum value
                10, // maximum value
                1); // step
        _haloSizeSpinner.setModel(haloSizeModel);
        _haloSizeSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getHaloRadius()));
        _haloSizeSpinner.addChangeListener(e -> {
            symbolizerWrapper.setHaloRadius(_haloSizeSpinner.getValue().toString());
        });

        String haloColorStr = symbolizerWrapper.getHaloColor();
        Color haloColor = ColorUtilities.fromHex(haloColorStr);
        GuiUtilities.colorButton(_haloColorButton, haloColor, MainController.COLOR_IMAGE_SIZE);
        _haloColorButton.setBackground(haloColor);
        _haloColorButton.addActionListener(e -> {
            Color initialBackground = _haloColorButton.getBackground();
            Color background = JColorChooser.showDialog(null, "Halo Color Chooser", initialBackground);
            if (background != null) {
                GuiUtilities.colorButton(_haloColorButton, background, MainController.COLOR_IMAGE_SIZE);
                symbolizerWrapper.setHaloColor(ColorUtilities.asHex(background));
            }
        });

        SpinnerModel displacementXModel = new SpinnerNumberModel(0, // initial value
                0, // minimum value
                100, // maximum value
                5); // step
        _displacementXSpinner.setModel(displacementXModel);
        _displacementXSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getDisplacementX()));
        _displacementXSpinner.addChangeListener(e -> {
            symbolizerWrapper.setDisplacementX(_displacementXSpinner.getValue().toString());
        });

        SpinnerModel displacementYModel = new SpinnerNumberModel(0, // initial value
                0, // minimum value
                100, // maximum value
                5); // step
        _displacementYSpinner.setModel(displacementYModel);
        _displacementYSpinner.setValue((int) Double.parseDouble(symbolizerWrapper.getDisplacementX()));
        _displacementYSpinner.addChangeListener(e -> {
            symbolizerWrapper.setDisplacementX(_displacementYSpinner.getValue().toString());
        });

        // TODO ANCHORS

    }

}
