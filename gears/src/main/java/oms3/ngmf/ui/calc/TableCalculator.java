///*
// * TableCalculator.java
// *
// * Created on April 25, 2007, 7:35 PM
// *
// * To change this template, choose Tools | Template Manager
// * and open the template in the editor.
// */
//package oms3.ngmf.ui.calc;
//
//import gnu.jel.CompilationException;
//import gnu.jel.CompiledExpression;
//import gnu.jel.Evaluator;
//import java.io.IOException;
//import java.io.InputStream;
//import javax.swing.JTable;
//import javax.swing.table.AbstractTableModel;
//
///**
// */
//public class TableCalculator {
//
//    String usage() {
//        try {
//            InputStream is = this.getClass().getResourceAsStream("/ngmf/ui/calc/Functions.txt");
//            if (is == null) {
//                return "";
//            }
//            int c;
//            StringBuffer b = new StringBuffer();
//            while ((c = is.read()) != -1) {
//                b.append((char) c);
//            }
//            is.close();
//            return b.toString();
//        } catch (IOException ex) {
//            return ex.getMessage();
//        }
//    }
//
//    public String apply(JelLibrarySupport jel, JTable table, String expr) {
//
//        AbstractTableModel model = (AbstractTableModel) table.getModel();
//
//        String lhs = null;
//        String rhs = expr;
//
//        if (expr.equals("help")) {
//            return usage();
//        }
//
//        int idx = expr.indexOf('=');
//        if ((idx > 0) &&
//                (expr.charAt(idx + 1) != '=') &&
//                (expr.charAt(idx - 1) != '!') &&
//                (expr.charAt(idx - 1) != '<') &&
//                (expr.charAt(idx - 1) != '>')) {
//            lhs = expr.substring(0, expr.indexOf('=')).trim();
//            rhs = expr.substring(expr.indexOf('=') + 1).trim();
//        }
//
//
//        expr = expr.trim();
//        if (expr.startsWith("=")) {
//            if (table.getSelectedColumns().length > 0 && table.getSelectedRows().length > 0) {
//                int[] cols = table.getSelectedColumns();
//                int[] rows = table.getSelectedRows();
//                for (int r = 0; r < rows.length; r++) {
//                    for (int c = 0; c < cols.length; c++) {
//                        model.setValueAt(expr.substring(1), rows[r], cols[c]);
//                        model.fireTableCellUpdated(rows[r], cols[c]);
//                    }
//                }
//            }
//        } else {
//            if (expr.startsWith("*") || expr.startsWith("/") || expr.startsWith("+") || expr.startsWith("-")) {
//                if (table.getSelectedColumns().length > 0 && table.getSelectedRows().length > 0) {
//                    int[] cols = table.getSelectedColumns();
//                    int[] rows = table.getSelectedRows();
//                    for (int r = 0; r < rows.length; r++) {
//                        for (int c = 0; c < cols.length; c++) {
//                            try {
//                                String val = table.getValueAt(rows[r], cols[c]).toString() + expr;
//                                CompiledExpression expr_c = Evaluator.compile(val, jel.getLibrary());
//                                Object result = expr_c.evaluate(jel.getContext());
//                                model.setValueAt(result, rows[r], cols[c]);
//                                model.fireTableCellUpdated(rows[r], cols[c]);
//                            } catch (Throwable ex) {
//                                return "Error " + ex.getMessage();
//                            }
//                        }
//                    }
//                    return "";
//                } else {
//                    return "Error : nothing selected in table";
//                }
//            }
//            try {
//                // test if implicite looping
//                if (expr.indexOf('$') == -1) {
//                    CompiledExpression expr_c = Evaluator.compile(rhs, jel.getLibrary());
//                    // single expression
//                    if (lhs == null) {
//                        try {
//                            Object result = expr_c.evaluate(jel.getContext());
//                            if (result != null) {
//                                return " Result of '" + rhs + "': " + result + "\n";
//                            }
//                        } catch (Throwable ex) {
//                            return "Error " + ex.getMessage();
//                        }
//                    }
//                } else if (lhs != null) {
//                    CompiledExpression expr_c = Evaluator.compile(rhs, jel.getLibrary());
//                    if (lhs.startsWith("$")) {
//                        lhs = lhs.substring(1);
//                        int col = Util.findColumn(model, lhs);
//                        if (col == -1) {
//                            return "No such column " + lhs + "\n";
//                        }
//                        for (int row = 0; row < model.getRowCount(); row++) {
//                            try {
//                                jel.getResolver().setRow(row);
//                                Object result = expr_c.evaluate(jel.getContext());
//                                if (result != null) {
//                                    model.setValueAt(result, row, col);
//                                    model.fireTableCellUpdated(row, col);
//                                }
//                            } catch (Throwable ex) {
//                                return "Error " + ex.getMessage();
//                            }
//                        }
//                    } else {
//                        return "lhs array not supported yet. \n";
//                    }
//                } else {
//                    // row selection
//                    CompiledExpression expr_c = Evaluator.compile(rhs, jel.getLibrary());
//                    table.clearSelection();
//                    table.setColumnSelectionAllowed(false);
//                    StringBuffer b = new StringBuffer();
//                    for (int row = 0; row < model.getRowCount(); row++) {
//                        try {
//                            jel.getResolver().setRow(row);
//                            Object result = expr_c.evaluate(jel.getContext());
//                            if (result != null) {
//                                if (result.getClass() == Boolean.class) {
//                                    boolean r = (Boolean) result;
//                                    if (r) {
//                                        table.addRowSelectionInterval(row, row);
//                                    }
//                                } else {
//                                    b.append(result);
//                                }
//                            }
//                        } catch (Throwable ex) {
//                            return "Error " + ex.getMessage();
//                        }
//                    }
//                    return b.toString();
//                }
//            } catch (CompilationException ce) {
//                StringBuffer b = new StringBuffer();
//                b.append("  ERROR: ");
//                b.append(ce.getMessage() + "\n");
//                b.append("                       ");
//                b.append(rhs + "\n");
//                int column = ce.getColumn(); // Column, where error was found
//                for (int i = 0; i <
//                        column + 23 - 1; i++) {
//                    b.append(' ');
//                }
//
//                b.append("^\n");
//                return b.toString();
//            }
//        }
//        return "";
//    }
//}
