package org.hortonmachine.style;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.style.TextSymbolizerWrapper;
import org.hortonmachine.gui.utils.GuiUtilities;

@SuppressWarnings("serial")
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
        _labelCombo.addActionListener(e -> {
            String field = _labelCombo.getSelectedItem().toString();
            symbolizerWrapper.setLabelName(field, true);
        });
        String fieldName = symbolizerWrapper.getLabelName();
        if (fieldName != null) {
            _labelCombo.setSelectedItem(fieldName);
            symbolizerWrapper.setLabelName(fieldName, true);
        } else {
            String defFieldName = _labelCombo.getSelectedItem().toString();
            symbolizerWrapper.setLabelName(defFieldName, true);
        }

        SpinnerModel opacityModel = new SpinnerNumberModel(255, // initial value
                0, // minimum value
                255, // maximum value
                5); // step
        _opacitySpinner.setModel(opacityModel);
        String opacityStr = symbolizerWrapper.getOpacity();
        if (opacityStr == null)
            opacityStr = "1";
        double opacity = Double.parseDouble(opacityStr);
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
        String rotation = symbolizerWrapper.getRotation();
        if (rotation == null)
            rotation = "0";
        _rotationSpinner.setValue((int) Double.parseDouble(rotation));
        _rotationSpinner.addChangeListener(e -> {
            symbolizerWrapper.setRotation(_rotationSpinner.getValue().toString(), false);
        });

        String fontFamily = symbolizerWrapper.getFontFamily();
        String fontSize = symbolizerWrapper.getFontSize();
        String fontStyle = symbolizerWrapper.getFontStyle();
//        String fontWeight = symbolizerWrapper.getFontWeight();

        Font decoded = Font.decode(fontFamily + "-" + fontStyle + "-" + fontSize);

        _fontButton.addActionListener(e -> {
            JFontChooser chooser = new JFontChooser(decoded);
            Font font2 = chooser.showDialog(this, "Choose a font");

            symbolizerWrapper.setFontSize(font2.getSize() + "");
            symbolizerWrapper.setFontFamily(font2.getFamily());
            symbolizerWrapper.setFontStyle(font2.isBold() ? "bold" : font2.isItalic() ? "italic" : "normal");
            symbolizerWrapper.setFontWeight(font2.isBold() ? "bold" : font2.isItalic() ? "italic" : "normal");
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
        String haloRadius = symbolizerWrapper.getHaloRadius();
        if (haloRadius == null) {
            haloRadius = "0";
        }
        _haloSizeSpinner.setValue((int) Double.parseDouble(haloRadius));
        _haloSizeSpinner.addChangeListener(e -> {
            symbolizerWrapper.setHaloRadius(_haloSizeSpinner.getValue().toString());
        });

        String haloColorStr = symbolizerWrapper.getHaloColor();
        if (haloColorStr == null)
            haloColorStr = "#FFFFFF";
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

        _anchorYCombo.setModel(new DefaultComboBoxModel(Alignments.toVerticalStrings()));
        String anchorY = symbolizerWrapper.getAnchorY();
        Alignments vAlign = Alignments.verticalAlignmentfromDouble(anchorY);
        _anchorYCombo.setSelectedItem(vAlign.toString());
        _anchorYCombo.addActionListener(e -> {
            String newAnchorY = _anchorYCombo.getSelectedItem().toString();

            Alignments alignment = Alignments.toAlignment(newAnchorY);
            symbolizerWrapper.setAnchorY(alignment.toDouble() + "");
        });

        _anchorXCombo.setModel(new DefaultComboBoxModel(Alignments.toHorizontalStrings()));
        String anchorX = symbolizerWrapper.getAnchorX();
        Alignments hAlign = Alignments.horizontalAlignmentfromDouble(anchorX);
        _anchorXCombo.setSelectedItem(hAlign.toString());
        _anchorXCombo.addActionListener(e -> {
            String newAnchorX = _anchorXCombo.getSelectedItem().toString();

            Alignments alignment = Alignments.toAlignment(newAnchorX);
            symbolizerWrapper.setAnchorX(alignment.toDouble() + "");
        });

        SpinnerModel displacementXModel = new SpinnerNumberModel(0, // initial value
                -100, // minimum value
                100, // maximum value
                5); // step
        _displacementXSpinner.setModel(displacementXModel);
        String displacementX = symbolizerWrapper.getDisplacementX();
        if (displacementX == null) {
            displacementX = "0";
        }
        _displacementXSpinner.setValue((int) Double.parseDouble(displacementX));
        _displacementXSpinner.addChangeListener(e -> {
            symbolizerWrapper.setDisplacementX(_displacementXSpinner.getValue().toString());
        });

        SpinnerModel displacementYModel = new SpinnerNumberModel(0, // initial value
                -100, // minimum value
                100, // maximum value
                5); // step
        _displacementYSpinner.setModel(displacementYModel);
        String displacementY = symbolizerWrapper.getDisplacementY();
        if (displacementY == null) {
            displacementY = "0";
        }
        _displacementYSpinner.setValue((int) Double.parseDouble(displacementY));
        _displacementYSpinner.addChangeListener(e -> {
            symbolizerWrapper.setDisplacementY(_displacementYSpinner.getValue().toString());
        });

        String perpendOffset = symbolizerWrapper.getPerpendicularOffset();
        _perpenOffsetText.setText(perpendOffset);
        _perpenOffsetText.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmp = _perpenOffsetText.getText();
                symbolizerWrapper.setPerpendicularOffset(tmp);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        String initialGap = symbolizerWrapper.getInitialGap();
        _initialGapText.setText(initialGap);
        _initialGapText.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmp = _initialGapText.getText();
                symbolizerWrapper.setInitialGap(tmp);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        /// VENDOR OPTIONS
        String _maxDispPixel = symbolizerWrapper.getMaxDisplacementVO();
        _voMaxDispPixelText.setText(_maxDispPixel);
        _voMaxDispPixelText.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpVo = _voMaxDispPixelText.getText();
                symbolizerWrapper.setMaxDisplacementVO(tmpVo);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        String autoWrapPixel = symbolizerWrapper.getAutoWrapVO();
        _voAutoWrapPixelsText.setText(autoWrapPixel);
        _voAutoWrapPixelsText.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpVo = _voAutoWrapPixelsText.getText();
                symbolizerWrapper.setAutoWrapVO(tmpVo);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        String spaceAroundPixel = symbolizerWrapper.getSpaceAroundVO();
        _voSpaceAroundPixelsText.setText(spaceAroundPixel);
        _voSpaceAroundPixelsText.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpVo = _voSpaceAroundPixelsText.getText();
                symbolizerWrapper.setSpaceAroundVO(tmpVo);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        String repeatVo = symbolizerWrapper.getRepeatVO();
        _voRepeatEveryPixelText.setText(repeatVo);
        _voRepeatEveryPixelText.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpVo = _voRepeatEveryPixelText.getText();
                symbolizerWrapper.setRepeatVO(tmpVo);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        String followLineVo = symbolizerWrapper.getFollowLineVO();
        _voFollowLineText.setText(followLineVo);
        _voFollowLineText.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpVo = _voFollowLineText.getText();
                symbolizerWrapper.setFollowLineVO(tmpVo);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        String maxAngleVo = symbolizerWrapper.getMaxAngleDeltaVO();
        _voMaxAngleAllowedText.setText(maxAngleVo);
        _voMaxAngleAllowedText.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpVo = _voMaxAngleAllowedText.getText();
                symbolizerWrapper.setMaxAngleDeltaVO(tmpVo);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

    }

}
