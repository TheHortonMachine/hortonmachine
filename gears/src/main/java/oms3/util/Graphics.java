/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.util;

import java.awt.GraphicsEnvironment;
import javax.swing.UIManager;

/**
 *
 * @author od
 */
public class Graphics {
    
    public static void nativeLF() {
        try {
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            String osName = System.getProperty("os.name");
            if ((osName != null) && osName.toLowerCase().startsWith("lin")) {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            }
        } catch (Exception E) {
        }
    }
}
