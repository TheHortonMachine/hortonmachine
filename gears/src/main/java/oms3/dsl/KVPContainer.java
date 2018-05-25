/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package oms3.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author od
 */
public class KVPContainer implements Buildable {

    List<KVP> entries = new ArrayList<KVP>();

    public List<KVP> getEntries() {
        return entries;
    }
    

    @Override
    public Buildable create(Object name, Object value) {
        entries.add(new KVP(name.toString(), value));
        return LEAF;
    }
}
