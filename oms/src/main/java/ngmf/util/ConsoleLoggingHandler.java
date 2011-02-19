/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ngmf.util;

import java.util.logging.ConsoleHandler;

/**
 *
 * @author od
 */
public class ConsoleLoggingHandler extends ConsoleHandler {

    public ConsoleLoggingHandler() {
       setOutputStream(System.out);
    }
}
