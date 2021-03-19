package org.hortonmachine.gforms;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

public class KeyComponent {
//    private String keyHint = "enter key";
    private List<String> keys;
    private JTextField keyTextField;
    private JComboBox<String> keyCombo;
//
//    public KeyComponent() {
//        keyTextField = new JTextField();
//        keyTextField.setText("enter key");
//    }
//
//    public KeyComponent( List<String> keys ) {
//        this.keys = keys;
//        keyCombo = new JComboBox<>(keys.toArray(new String[0]));
//    }

    public KeyComponent( IFormHandler formHandler ) {
        List<String> formKeys = formHandler.getFormKeys();
        if (formKeys == null) {
            keyTextField = new JTextField();
            keyTextField.setText("enter key");
        } else {
            keyCombo = new JComboBox<>(formKeys.toArray(new String[0]));
        }
    }

    public String getText() {
        if (keyTextField != null) {
            return keyTextField.getText();
        } else {
            return keyCombo.getSelectedItem().toString();
        }
    }

    public JComponent getComponent() {
        if (keyTextField != null) {
            return keyTextField;
        } else {
            return keyCombo;
        }
    }

}
