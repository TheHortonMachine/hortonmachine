package oms3.dsl.analysis;

import java.io.File;
import oms3.dsl.*;
import oms3.ngmf.ui.graph.ValueSet;
import oms3.ngmf.util.OutputStragegy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author od
 */
public class FlowDur implements Buildable {

    List<ValueSet> y = new ArrayList<ValueSet>();
    String title = "Flow Duration";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    List<ValueSet> getY() {
        List<ValueSet> fd = new ArrayList<ValueSet>();
        for (ValueSet valueSet : y) {
            fd.add(new VS(valueSet));
        }
        return fd;
    }

    /** Adapter
     * 
     */
    static class VS implements ValueSet {

        ValueSet v;

        VS(ValueSet v) {
            this.v = v;
        }

        @Override
        public Double[] getDoubles(File st, String simName) throws IOException {
            Double[] duration = new Double[100];
            Double[] y = v.getDoubles(st, simName);
            Arrays.sort(y);
            int l1 = y.length / 100;
            for (int i2 = 0; i2 < 100; i2++) {
                duration[99 - i2] = y[(i2 + 1) * l1];
            }
            return duration;
        }

        @Override
        public String getName() {
            return v.getName();
        }

        @Override
        public boolean isLine() {
            return true;
        }

        @Override
        public boolean isShape() {
            return false;
        }
    }

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("y")) {
            Axis a = new Axis();
            y.add(a);
            return a;
        }
        throw new IllegalArgumentException("flowduration cannot handle :" + name);
    }
}
