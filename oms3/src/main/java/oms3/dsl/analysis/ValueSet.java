/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package oms3.dsl.analysis;

import java.io.IOException;
import ngmf.util.OutputStragegy;

/**
 *
 * @author od
 */
public interface ValueSet {
     Double[] getDoubles(OutputStragegy st, String simName) throws IOException;
     String   getName();
}
