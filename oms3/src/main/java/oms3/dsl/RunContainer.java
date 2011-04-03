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
public class RunContainer implements Buildable {

    List<Exec> l = new ArrayList<Exec>();

    @Override
    public Buildable create(Object name, Object value) {
        Exec.Type type = null;
        if (name.equals("ant")) {
            type = Exec.Type.ANT;
        } else if (name.equals("groovy")) {
            type = Exec.Type.GROOVY;
        } else {
            throw new IllegalArgumentException(name.toString());
        }
        Exec e = new Exec(type);
        l.add(e);
        return e;
    }

    void run() throws Exception {
        for (Exec exec : l) {
            exec.run();
        }
    }

}
