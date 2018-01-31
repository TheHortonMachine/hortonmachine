/*
 * Util.java
 *
 * Created on April 25, 2007, 8:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package oms3.ngmf.ui.calc;

import javax.swing.table.TableModel;

/**
 *
 * @author od
 */
public class Util {
    
    /** Creates a new instance of Util */
    private Util() {
    }
    
    static int findColumn(TableModel model, String name) {
        for (int i = 0; i<model.getColumnCount(); i++) {
            if (name.equals(model.getColumnName(i))) {
                return i;
            }
        }
        return -1;
    }
    
}
