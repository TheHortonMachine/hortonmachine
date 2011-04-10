/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package oms3.dsl;

/**
 *
 * @author od
 */
public class Ontology implements Buildable {

    String file;
    String type;

    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFile() {
        return file;
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }
}
