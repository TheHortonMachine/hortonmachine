package oms3.dsl.analysis;

import oms3.dsl.*;
import oms3.ngmf.ui.graph.ValueSet;

/**
 * 
 * @author od
 */
public class Scatter implements Buildable {

    Axis y;
    Axis x;
    String title = "Scatter Plot";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    ValueSet getY() {
        return y;
    }

    ValueSet getX() {
        return x;
    }

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("y")) {
            return y = new Axis();
        }
        if (name.equals("x")) {
            return x = new Axis();
        }
        throw new IllegalArgumentException("scatterplot cannot handle :" + name);
    }
}
