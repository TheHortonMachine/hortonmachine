package org.hortonmachine.style;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.hortonmachine.gears.utils.style.FeatureTypeStyleWrapper;

@SuppressWarnings("serial")
public class FeatureTypeParametersController extends FeatureTypeParametersView {
    private FeatureTypeStyleWrapper ftsWrapper;

    /**
    * Default constructor
     * @param mainController 
    */
    public FeatureTypeParametersController( FeatureTypeStyleWrapper ftsWrapper, MainController mainController ) {
        this.ftsWrapper = ftsWrapper;
        _applyButton.addActionListener(e -> {
            mainController.applyStyle();
        });
        init();
    }

    private void init() {
        String name = ftsWrapper.getName();
        _nameTextField.setText(name);
        _nameTextField.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                String tmpName = _nameTextField.getText();
                ftsWrapper.setName(tmpName);
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

    }

}
