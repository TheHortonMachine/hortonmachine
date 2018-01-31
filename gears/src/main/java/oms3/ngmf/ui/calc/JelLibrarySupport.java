///*
// * JelLibrarySupport.java
// *
// * Created on April 26, 2007, 9:52 AM
// *
// * To change this template, choose Tools | Template Manager
// * and open the template in the editor.
// */
//package oms3.ngmf.ui.calc;
//
//import oms3.util.Stats;
//import gnu.jel.CompilationException;
//import gnu.jel.Library;
//import javax.swing.table.TableModel;
//
///**
// *
// * @author od
// */
//public class JelLibrarySupport {
//
//    TableResolver symtab = new TableResolver();
//    Object[] context = {symtab};
//    Library lib = new Library(new Class[]{
//                Math.class, Mathx.class, Stats.class
//            },
//            new Class[]{TableResolver.class}, null, symtab, null);
//    TableModel model;
//
//    /** Creates a new instance of TableCalculator
//     * @param model 
//     */
//    public JelLibrarySupport(TableModel model) {
//        this.model = model;
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
//    public void setModel(TableModel model) {
//        this.model = model;
//    }
//
//    Library getLibrary() {
//        return lib;
//    }
//
//    Object[] getContext() {
//        return context;
//    }
//
//    TableResolver getResolver() {
//        return symtab;
//    }
//
//    public class TableResolver extends gnu.jel.DVMap {
//
//        int row;
//
//        void setRow(int row) {
//            this.row = row;
//        }
//        // used by the compiler to query about available dynamic
//        // variables
//
//        @Override
//        public String getTypeName(String name) {
//            boolean isArray = true;
//            if (name.startsWith("$")) {
//                isArray = false;
//                name = name.substring(1);
//            }
//            int col = Util.findColumn(model, name);
//            if (col == -1) {
//                return null;
//            }
//
//            if (!isArray && Number.class.isAssignableFrom(model.getColumnClass(col))) {
//                return "Number";
//            }
//            if (isArray && Number.class.isAssignableFrom(model.getColumnClass(col))) {
//                return "NumberArray";
//            }
//            if (!isArray && model.getColumnClass(col) == String.class) {
//                return "String";
//            }
//            if (isArray && model.getColumnClass(col) == String.class) {
//                return "StringArray";
//            }
//            return null;
//        }
//
//        public double getNumberProperty(String name) {
//            int col = Util.findColumn(model, name.substring(1));
//            return ((Number) model.getValueAt(row, col)).doubleValue();
//        }
//
//        public double[] getNumberArrayProperty(String name) {
//            int col = Util.findColumn(model, name);
//            double[] a = new double[model.getRowCount()];
//            for (int i = 0; i < a.length; i++) {
//                a[i] = ((Number) model.getValueAt(i, col)).doubleValue();
//            }
//            return a;
//        }
//
//        public double getStringProperty(String name) {
//            int col = Util.findColumn(model, name.substring(1));
//            return Double.parseDouble(model.getValueAt(row, col).toString());
//        }
//
//        public double[] getStringArrayProperty(String name) {
//            int col = Util.findColumn(model, name);
//            double[] a = new double[model.getRowCount()];
//            for (int i = 0; i < a.length; i++) {
//                a[i] = Double.parseDouble(model.getValueAt(i, col).toString());
//            }
//            return a;
//        }
//    }
//}
