package org.hortonmachine.style;

import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.hortonmachine.gears.utils.style.RuleWrapper;
import org.opengis.filter.Filter;

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
            try {
                String filterText = _filterTextArea.getText().trim();
                if (filterText.length() > 0) {
                    Filter filter = ECQL.toFilter(filterText);
                    ruleWrapper.getRule().setFilter(filter);
                }else {
                    ruleWrapper.getRule().setFilter(null);
                }
            } catch (CQLException e1) {
                e1.printStackTrace();
            }

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

        _filterTextArea.setMargin(new Insets(10, 10, 10, 10));

        Filter filter = ruleWrapper.getRule().getFilter();
        if (filter != null) {
            _filterTextArea.setText(ECQL.toCQL(filter));
        } else {
            _filterTextArea.setText(""); //$NON-NLS-1$
        }
    }

    private String checkScale( String scale ) {
        try {
            double scaleDouble = Double.parseDouble(scale);
            if (scaleDouble < 0) {
                scaleDouble = 1;
            }
            return String.valueOf(scaleDouble);
        } catch (NumberFormatException e) {
            return null;
        }

    }

}
