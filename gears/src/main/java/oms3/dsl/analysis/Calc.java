///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package oms3.dsl.analysis;
//
//import oms3.dsl.*;
//import oms3.ngmf.ui.calc.Mathx;
//import oms3.ngmf.ui.graph.ValueSet;
//import oms3.ngmf.util.OutputStragegy;
//import gnu.jel.CompilationException;
//import gnu.jel.CompiledExpression;
//import gnu.jel.Evaluator;
//import gnu.jel.Library;
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//import oms3.util.Stats;
//
///**
// *
// * @author od
// */
//public class Calc implements Buildable, ValueSet {
//
//    Map<String, Double[]> m = new HashMap<String, Double[]>();
//    Map<String, Axis> ma = new HashMap<String, Axis>();
//    Resolver symtab = new Resolver();
//    Object[] context = {symtab};
//    Library lib = new Library(new Class[]{Math.class, Mathx.class, Stats.class},
//            new Class[]{Resolver.class}, null, symtab, null);
//    static final String DOUBLE = "D";
//    //
//    String eq;
//    boolean acc = false;
//    String title;
//
//    boolean shape = false;
//    boolean line = true;
//    
//    Calc() {
//        try {
//            lib.markStateDependent("random", null);
//            lib.markStateDependent("random", new Class[]{double.class, double.class});
//            lib.markStateDependent("ramp", new Class[]{double.class, double.class});
//            lib.markStateDependent("reset_ramp", null);
//        } catch (CompilationException ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    public void setLine(boolean line) {
//        this.line = line;
//    }
//
//    public void setShape(boolean shape) {
//        this.shape = shape;
//    }
//
//    @Override
//    public boolean isLine() {
//        return line;
//    }
//    
//    @Override
//    public boolean isShape() {
//        return shape;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    public void setEq(String eq) {
//        this.eq = eq;
//    }
//
//    public void setAcc(boolean acc) {
//        this.acc = acc;
//    }
//
//    public class Resolver extends gnu.jel.DVMap {
//
//        int row;
//
//        void setRow(int row) {
//            this.row = row;
//        }
//
//        @Override
//        public String getTypeName(String name) {
//            if (m.get(name) != null) {
//                return DOUBLE;
//            }
//            return null;
//        }
//
//        public double getDProperty(String name) {
//            return m.get(name)[row].doubleValue();
//        }
//    }
//
//    @Override
//    public Buildable create(Object name, Object value) {
//        Axis a = new Axis();
//        ma.put(name.toString(), a);
//        return a;
//    }
//
//    @Override
//    public Double[] getDoubles(File st, String simName) throws IOException {
//        if (eq == null) {
//            throw new IllegalArgumentException("missing equation in 'eq'");
//        }
//        int len = -1;
//        for (String key : ma.keySet()) {
//            Axis a = ma.get(key);
//            Double[] v = a.getDoubles(st, simName);
//            if (len == -1) {
//                len = v.length;
//            } else {
//                if (len != v.length) {
//                    throw new IllegalArgumentException("array length problem: " + key);
//                }
//            }
//            m.put(key, v);
//        }
//
//        Double[] result = new Double[len];
//        try {
//            CompiledExpression expr_c = Evaluator.compile(eq.trim(), lib);
//            try {
//                double sum = 0;
//                for (int row = 0; row < len; row++) {
//                    symtab.setRow(row);
//                    Object r = expr_c.evaluate(context);
//                    if (r != null) {
//                        sum += (Double) r;
//                        result[row] = acc ? sum : (Double) r;
//                    }
//                }
//            } catch (Throwable t) {
//                System.out.println(t.getMessage());
//            }
//        } catch (CompilationException ce) {
//            StringBuffer b = new StringBuffer();
//            b.append("  ERROR: ");
//            b.append(ce.getMessage() + "\n");
//            b.append("                       ");
//            b.append(eq + "\n");
//            int column = ce.getColumn(); // Column, where error was found
//            for (int i = 0; i < column + 23 - 1; i++) {
//                b.append(' ');
//            }
//            b.append("^\n");
//            System.out.println(b.toString());
//        }
//        return result;
//    }
//
//    @Override
//    public String getName() {
//        return title == null ? ("Equation '" + eq + "' " + (acc ? "(accumulated)" : "")) : title;
//    }
//}
