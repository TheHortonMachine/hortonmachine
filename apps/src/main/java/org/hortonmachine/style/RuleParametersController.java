package org.hortonmachine.style;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.hortonmachine.gears.utils.style.RuleWrapper;

@SuppressWarnings("serial")
public class RuleParametersController extends RuleParametersView {
    private RuleWrapper ruleWrapper;

    /**
    * Default constructor
     * @param mainController 
    */
    public RuleParametersController( RuleWrapper ruleWrapper, MainController mainController ) {
        this.ruleWrapper = ruleWrapper;
        _applyButton.addActionListener(e -> {
            mainController.applyStyle();
        });
        init();
    }

    private void init() {
        String name = ruleWrapper.getName();
        _nameTextField.setText(name);
        _nameTextField.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpName = _nameTextField.getText();
                ruleWrapper.setName(tmpName);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        String minScale = ruleWrapper.getMinScale();
        _minScaleTextField.setText(minScale);
        _minScaleTextField.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpMinScale = _minScaleTextField.getText();

                tmpMinScale = checkScale(tmpMinScale);
                if (tmpMinScale != null)
                    ruleWrapper.setMinScale(tmpMinScale);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        String maxScale = ruleWrapper.getMaxScale();
        _maxScaleTextField.setText(maxScale);
        _maxScaleTextField.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpMaxScale = _maxScaleTextField.getText();

                tmpMaxScale = checkScale(tmpMaxScale);
                if (tmpMaxScale != null)
                    ruleWrapper.setMaxScale(tmpMaxScale);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });
    }

    private String checkScale( String scale ) {
        try {
            int scaleInt = Integer.parseInt(scale);
            if (scaleInt < 0) {
                scaleInt = 1;
            }
            return String.valueOf(scaleInt);
        } catch (NumberFormatException e) {
            return null;
        }

    }

}
