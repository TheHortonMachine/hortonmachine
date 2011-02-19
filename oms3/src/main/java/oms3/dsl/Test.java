package oms3.dsl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import oms3.ComponentAccess;
import oms3.annotations.*;
import ngmf.util.OutputStragegy;
import oms3.util.Ranges;
import ngmf.util.SimpleDirectoryOutput;

public class Test implements Buildable, Runnable {

    private static final Logger log = Logger.getLogger("oms3.sim");
    private Random rand = new Random();

    public static abstract class Sample implements Buildable {

        String name;
        double min;
        double max;

        public Sample(String name) {
            this.name = name;
        }

        public void setMin(double min) {
            this.min = min;
        }

        public void setLower(double min) {
            this.min = min;
        }

        public void setMax(double max) {
            this.max = max;
        }

        public void setUpper(double max) {
            this.max = max;
        }

        public double getMax() {
            return max;
        }

        public double getMin() {
            return min;
        }

        public String getName() {
            return name;
        }

        // called by groovy on xxx(abc:v) value
        public void call(Object value) {
            throw new UnsupportedOperationException("Illegal.");
        }

        @Override
        public Buildable create(Object name, Object value) {
            return LEAF;
        }

        public abstract boolean next();
    }

    public class Sampling implements Buildable {

        List<Sample> l = new ArrayList<Sample>();

        @Override
        public Buildable create(Object name, Object value) {
            Sample p = new Sample(name.toString()) {

                @Override
                public boolean next() {
                    Ranges.Gen in = new Ranges.Gen(comp, getName(), getMin(), getMax());
                    in.next(rand);
                    return true;
                }
            };
            l.add(p);
            return p;
        }
    }

    public class Checking implements Buildable {

        List<Sample> l = new ArrayList<Sample>();

        @Override
        public Buildable create(Object name, Object value) {
            Sample p = new Sample(name.toString()) {

                @Override
                public boolean next() {
                    Ranges.Check out = new Ranges.Check(comp, getName(), getMin(), getMax());
                    return out.check();
                }
            };
            l.add(p);
            return p;
        }
    }
    //
    Object comp;
    Model model;
    String name;
    OutputDescriptor output;
    Sampling sample;
    Checking checking;
    int count;

    public void setCount(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count>=1!");
        }
        this.count = count;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("model")) {
            if (model != null) {
                throw new IllegalArgumentException("Only one 'model' allowed.");
            }
            return model = new Model();
        } else if (name.equals("output")) {
            return output = new OutputDescriptor();
        } else if (name.equals("sample")) {
            return sample = new Sampling();
        } else if (name.equals("check")) {
            return checking = new Checking();
        } else {
            throw new IllegalArgumentException(name.toString());
        }
    }

    @Override
    public void run() {
        try {
            comp = model.getComponent();

            OutputStragegy st;
            if (output == null) {
                st = new SimpleDirectoryOutput(new File(System.getProperty("user.dir")), getName());
            } else {
                st = output.getOutputStrategy(getName());
            }

            // setting the input data;
            Map<String, Object> parameter = null;
            parameter = model.getParameter();

            ComponentAccess.setInputData(parameter, comp, log);
            ComponentAccess.adjustOutputPath(st.nextOutputFolder(), comp, log);

            ComponentAccess.callAnnotated(comp, Initialize.class, true);
            for (int i = 0; i < count; i++) {
                // create samples
                for (Sample s : sample.l) {
                    s.next();
                }
                ComponentAccess.callAnnotated(comp, Execute.class, false);
                // check samples
                for (Sample s : checking.l) {
                    boolean result = s.next();
                    System.out.print(result ? '.' : 'F');
                    System.out.flush();
                }
            }

            // execute and be done.
            ComponentAccess.callAnnotated(comp, Finalize.class, true);
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }
}
