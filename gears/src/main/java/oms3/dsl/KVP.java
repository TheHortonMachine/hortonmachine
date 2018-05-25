/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package oms3.dsl;

/**
 *
 * @author od
 */
public class KVP {

    String key;
    Object value;

    public KVP(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

}
