/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.ui.graph;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author od
 */
public interface ValueSet {

    Double[] getDoubles(File file, String simName) throws IOException;

    String getName();
    
    boolean isLine();

    boolean isShape();
}
