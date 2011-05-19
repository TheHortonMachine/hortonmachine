/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package oms3.dsl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/** Model Logging configuration.
 *
 * @author od
 */
public class Logging implements Buildable {

    // name -> loglevel
    Map<String, String> comps = new HashMap<String, String>();

    // the default log level for all components
    String all = Level.WARNING.getName();

    @Override
    public Buildable create(Object name, Object value) {
        comps.put(name.toString(), value.toString());
        return LEAF;
    }

    public void setAll(String all) {
        this.all = all;
    }

    public String getAll() {
        return all;
    }

    public Map<String, String> getCompLevels() {
        return comps;
    }
}