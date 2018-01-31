///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//
///*
// * PEditor.java
// *
// * Created on Jun 16, 2009, 4:26:58 PM
// */
//package oms3.ngmf.ui;
//
//import java.awt.Color;
//import java.awt.Component;
//import java.awt.Dimension;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.awt.event.MouseAdapter;
//import java.awt.event.MouseEvent;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.text.DecimalFormat;
//import java.text.ParseException;
//import java.util.List;
//import javax.swing.AbstractListModel;
//import javax.swing.ImageIcon;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JList;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.ListCellRenderer;
//import javax.swing.ListSelectionModel;
//import javax.swing.RowSorter;
//import javax.swing.UIManager;
//import javax.swing.event.ListSelectionEvent;
//import javax.swing.event.ListSelectionListener;
//import javax.swing.table.AbstractTableModel;
//import javax.swing.table.DefaultTableCellRenderer;
//import javax.swing.table.JTableHeader;
//import javax.swing.table.TableColumnModel;
//import javax.swing.table.TableModel;
//import javax.swing.table.TableRowSorter;
//
//import oms3.util.Stats;
//import oms3.io.CSProperties;
//import oms3.io.DataIO;
//import oms3.ngmf.ui.calc.JelLibrarySupport;
//import oms3.ngmf.ui.calc.TableCalculator;
//
///**
// *
// * @author od
// */
//public class PEditor extends javax.swing.JPanel implements CommandHandler {
//
//    private static final long serialVersionUID = 9177364340914985294L;
//    //
//    JConsolePanel cp = new JConsolePanel();
//    JTable table = new JTable();
//    TableListener listener = new TableListener(table);
//    JScrollPane sp = new JScrollPane(table);
//    CSProperties p;
//    TableCalculator calc = new TableCalculator();
//    JelLibrarySupport jel = new JelLibrarySupport(table.getModel());
//
//    public PEditor(List<File> file) {
//        this(file.toArray(new File[0]));
//    }
//
//    public PEditor(File[] file) {
//        initComponents();
//        setupComponents();
//        for (File f : file) {
//            fileCombo.addItem(f);
//        }
//    }
//
//    private void setupComponents() {
//        cp.setCommandHandler(this);
//        cp.setRows(6);
//        cp.getOut().println("Parameter Console, type 'help' for commands.");
//        cp.prompt();
//
//        JScrollPane sp1 = new JScrollPane();
//        sp1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
//        sp1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//        sp1.setViewportView(cp);
//        sp1.setMinimumSize(new Dimension(100, 100));
//
//        fileCombo.addItemListener(new ItemListener() {
//
//            public void itemStateChanged(ItemEvent e) {
//                if (e.getStateChange() == ItemEvent.SELECTED) {
//                    showP((File) e.getItem());
//                }
//            }
//        });
//
//        saveButton.setIcon(new ImageIcon(PEditor.class.getResource("/ngmf/ui/save-16x16.png")));
//        saveButton.setText("");
//        saveButton.setToolTipText("Save.");
//        saveButton.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent e) {
//                File f = (File) fileCombo.getSelectedItem();   //TODO save all 
//                copy(f, new File(f.toString() + "~"));
//                DataIO.save(p, f, "Parameter");
//            }
//        });
//
//        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
//        table.setColumnSelectionAllowed(true);
//        table.setRowSelectionAllowed(true);
//        table.setDoubleBuffered(true);
//        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
//        table.setIntercellSpacing(new Dimension(3, 1));
//        table.setDefaultRenderer(Object.class, new MyCellRenderer());
//        table.setShowHorizontalLines(false);
//        table.setShowVerticalLines(true);
//        table.setGridColor(Color.lightGray);
//
//        ListSelectionListener lsl = new ListSelectionListener() {
//
//            public void valueChanged(ListSelectionEvent e) {
//                if (table.getClientProperty("KV") != null) {
//                    return;
//                }
//                if (table.getRowSelectionAllowed() && table.getColumnSelectionAllowed() &&
//                        !e.getValueIsAdjusting()) {
//                    int[] scols = table.getSelectedColumns();
//                    int[] srows = table.getSelectedRows();
//                    if (srows.length * scols.length < 2) {
//                        stats(null);
//                        return;
//                    }
//                    double[] v = new double[srows.length * scols.length];
//                    int vi = 0;
//                    for (int i = 0; i < srows.length; i++) {
//                        int j = srows[i];
//                        for (int k = 0; k < scols.length; k++) {
//                            int l = scols[k];
//                            Object o = table.getValueAt(j, l);
//                            try {
//                                v[vi++] = Double.parseDouble(o.toString());
//                            } catch (NumberFormatException E) {
//                                stats(null);
//                                return;
//                            }
//                        }
//                    }
//                    stats(v);
//                }
//            }
//        };
//
//        table.getSelectionModel().addListSelectionListener(lsl);
//        table.getColumnModel().getSelectionModel().addListSelectionListener(lsl);
//
//        table.getTableHeader().addMouseListener(new MouseAdapter() {
//
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (table.getClientProperty("KV") != null) {
//                    return;
//                }
//                JTableHeader header = table.getTableHeader();
//                TableColumnModel columns = header.getColumnModel();
//
//                if (!columns.getColumnSelectionAllowed()) {
//                    return;
//                }
//
//                int column = header.columnAtPoint(e.getPoint());
//                if (column == -1) {
//                    return;
//                }
//
//                int count = table.getRowCount();
//                if (count != 0) {
//                    table.setRowSelectionInterval(0, count - 1);
//                }
//
//                ListSelectionModel selection = columns.getSelectionModel();
//                if (e.isShiftDown()) {
//                    int anchor = selection.getAnchorSelectionIndex();
//                    int lead = selection.getLeadSelectionIndex();
//                    if (anchor != -1) {
//                        boolean old = selection.getValueIsAdjusting();
//                        selection.setValueIsAdjusting(true);
//                        boolean anchorSelected = selection.isSelectedIndex(anchor);
//
//                        if (lead != -1) {
//                            if (anchorSelected) {
//                                selection.removeSelectionInterval(anchor, lead);
//                            } else {
//                                selection.addSelectionInterval(anchor, lead);
//                            }
//                        }
//                        if (anchorSelected) {
//                            selection.addSelectionInterval(anchor, column);
//                        } else {
//                            selection.removeSelectionInterval(anchor, column);
//                        }
//                        selection.setValueIsAdjusting(old);
//                    } else {
//                        selection.setSelectionInterval(column, column);
//                    }
//                } else if (e.isControlDown()) {
//                    if (selection.isSelectedIndex(column)) {
//                        selection.removeSelectionInterval(column, column);
//                    } else {
//                        selection.addSelectionInterval(column, column);
//                    }
//                } else {
////                    selection.setSelectionInterval(column, column);
//                    table.setColumnSelectionInterval(column, column);
//                }
//            }
//        });
//
//        table.addMouseListener(new MouseAdapter() {
//
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                table.setColumnSelectionAllowed(true);   // switches back from query selection
//            }
//        });
//
//        final JList rowHeader = new JList(new AbstractListModel() {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public int getSize() {
//                return table.getModel().getRowCount();
//            }
//
//            @Override
//            public Object getElementAt(int index) {
//                return new Integer(index);
//            }
//        });
//        rowHeader.setFixedCellWidth(30);
//        rowHeader.setFixedCellHeight(table.getRowHeight());
//        rowHeader.setCellRenderer(new RowHeaderRenderer(table));
//        rowHeader.setOpaque(false);
//        sp.setRowHeaderView(rowHeader);
//
//        rowHeader.addMouseListener(new MouseAdapter() {
//
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (table.getClientProperty("KV") != null) {
//                    return;
//                }
//
//                //select all columns
//                if (table.getColumnCount() != 0) {
//                    table.setColumnSelectionInterval(0, table.getColumnCount() - 1);
//                }
//
//                //get recent aef
//                int row = rowHeader.getSelectionModel().getLeadSelectionIndex();
//
//                if (row == -1) {
//                    return;
//                }
//
//                ListSelectionModel selection = rowHeader.getSelectionModel();
//                if (e.isShiftDown()) {
//                    int anchor = selection.getAnchorSelectionIndex();
//                    int lead = selection.getLeadSelectionIndex();
//                    if (anchor != -1) {
//                        boolean anchorSelected = table.isRowSelected(anchor);
//                        if (lead != -1) {
//                            if (anchorSelected) {
//                                table.removeRowSelectionInterval(anchor, lead);
//                            } else {
//                                table.addRowSelectionInterval(anchor, lead);
//                            }
//                        }
//                        if (anchorSelected) {
//                            table.addRowSelectionInterval(anchor, row);
//                        } else {
//                            table.removeRowSelectionInterval(anchor, row);
//                        }
//                    } else {
//                        table.setRowSelectionInterval(row, row);
//                    }
//                } else if (e.isControlDown()) {
//                    if (table.isRowSelected(row)) {
//                        table.removeRowSelectionInterval(row, row);
//                    } else {
//                        table.addRowSelectionInterval(row, row);
//                    }
//                } else {
//                    table.setRowSelectionInterval(row, row);
//                }
//
//            }
//        });
//
//        dimCombo.addItemListener(new ItemListener() {
//
//            public void itemStateChanged(ItemEvent e) {
//                if (e.getStateChange() == ItemEvent.SELECTED) {
//                    String item = e.getItem().toString().trim();
//                    if (item.contains("----")) {
//                        return;
//                    }
//                    AbstractTableModel model = null;
//                    // ALL
//                    if (dimCombo.getSelectedIndex() == 1) {
//                        model = DataIO.getProperties(p);
//                        setTableModel(model);
//                        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
//                        table.getColumnModel().getColumn(0).setPreferredWidth(125);
//                        table.getColumnModel().getColumn(0).setMaxWidth(125);
//                        RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
//                        table.setRowSorter(sorter);
//                        table.putClientProperty("KV", Boolean.TRUE);
//                    } else if (dimCombo.getSelectedIndex() == 2) {
//                        try {
//                            model = DataIO.getUnBoundProperties(p);
//                            setTableModel(model);
//                            table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
//                            table.getColumnModel().getColumn(0).setPreferredWidth(125);
//                            table.getColumnModel().getColumn(0).setMaxWidth(125);
//                            RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
//                            table.setRowSorter(sorter);
//                            table.putClientProperty("KV", Boolean.TRUE);
//                        } catch (ParseException E) {
//                            E.printStackTrace();
//                            return;
//                        }
//                        // 1D stuff
//                    } else if (dimCombo.getSelectedIndex() > 3) {
//                        try {
//                            if (DataIO.playsRole(p, item, "dimension")) {
//                                model = DataIO.getBoundProperties(p, item);
//                            } else if (DataIO.isBound(p, item, 2)) {
//                                model = DataIO.get2DBounded(p, item);
//                            }
//                            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//                            table.setRowSorter(null);
//                            setTableModel(model);
//                            table.putClientProperty("KV", null);
//                        } catch (ParseException E) {
//                            E.printStackTrace();
//                            return;
//                        }
//                    }  // TODO 2D model
//                }
//            }
//        });
//
//        split.setTopComponent(sp1);
//        split.setBottomComponent(sp);
//        split.setOneTouchExpandable(true);
//        stats(null);
//    }
//    static DecimalFormat fmt = new DecimalFormat("#.#####");
//
//    void stats(double[] v) {
//
//        if (v == null) {
//            numLabel.setText(" ");
//        } else {
//            double min = Stats.min(v);
//            double max = Stats.max(v);
//            double mean = Stats.mean(v);
//            double range = Stats.range(v);
//            double median = Stats.median(v);
//            double sum = Stats.sum(v);
////            double dev = Stats.stddev(v);
////            double var = Stats.variance(v);
//            numLabel.setText("#" + v.length + "   Sum[" + fmt.format(sum) + "] Min[" + fmt.format(min) + "]  Max[" + fmt.format(max) + "]  Mean[" + fmt.format(mean) + "]  Range[" +
//                    fmt.format(range) + "]  Median[" + fmt.format(median) + "]");
//            //Std[" + dev + "]  Var[" + var + "]");
//        }
//    }
//
//    private static void copy(File f1, File f2) {
//        try {
//            InputStream in = new FileInputStream(f1);
//            OutputStream out = new FileOutputStream(f2);
//
//            byte[] buf = new byte[4096];
//            int len;
//            while ((len = in.read(buf)) > 0) {
//                out.write(buf, 0, len);
//            }
//            in.close();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public String handle(String cmd) {
//        if (cmd.equals("clear")) {
//            cp.clear();
//            return "";
//        } else if (cmd.startsWith("new")) {
//            return handleCreate(cmd);
//        } else if (cmd.startsWith("del")) {
//            return handleDelete(cmd);
//        } else if (cmd.startsWith("con")) {
//            return handleConv(cmd);
//        }
//        return calc.apply(jel, table, cmd);
//    }
//
//    private String handleConv(String cmd) {
//        String[] arg = cmd.split("\\s+");
//        if (arg.length == 1) {
//            return " usage: con <file>.[statvar|params|data]\n";
//        }
//        if (arg[1].endsWith("statvar"))  {
//            return Convert.statvar(arg[1]);
//        } else if (arg[1].endsWith("params")) {
//            return Convert.param(arg[1]);
//        } else if (arg[1].endsWith("data")) {
//            return Convert.data(arg[1]);
//        }
//        return " cannot convert: " + arg[1];
//    }
//
//    private String handleCreate(String cmd) {
//        String[] arg = cmd.split("\\s+");
//        if (arg.length == 1) {
//            return " usage: new [param | dim | array]\n";
//        }
//        if (arg[1].startsWith("p")) {               // create param
//            if (arg.length < 3) {
//                return " usage: new param <name>...\n";
//            }
//            for (int i = 2; i < arg.length; i++) {
//                p.put(arg[i], "");
//            }
//        } else if (arg[1].startsWith("d")) {        // create dim
//            if (arg.length != 3) {
//                return " usage: new dim <name>\n";
//            }
//            String name = arg[2];
//            p.put(name, "");
//            p.getInfo(name).put("role", "dimension");
//        } else if (arg[1].startsWith("a")) {        // create array
//            if (arg.length < 4) {
//                return " usage: new array <name> <dim>\n";
//            }
//            String name = arg[2];
//            p.put(name, "");
//            p.getInfo(name).put("bound", arg[3]);
//        } else {
//            return " usage: new [param | dim | array]\n";
//        }
//        ((AbstractTableModel) table.getModel()).fireTableDataChanged();
//        sp.repaint();
//        return "";
//    }
//
//    private String handleDelete(String cmd) {
//        String[] arg = cmd.split("\\s+");            // delete entry
//        if (arg.length < 2) {
//            return " usage: del <name>...\n";
//        }
//        for (int i = 1; i < arg.length; i++) {
//            p.remove(arg[i]);
//        }
//        ((AbstractTableModel) table.getModel()).fireTableDataChanged();
//        sp.repaint();
//        return "";
//    }
//
//    private void setTableModel(AbstractTableModel model) {
//        table.setModel(model);
//        jel.setModel(model);
//        model.fireTableDataChanged();
//        sp.repaint();
//    }
//
//    private void showP(File file) {
//        try {
//            p = DataIO.properties(new FileReader(file), "Parameter");
//            List<String> dims = DataIO.keysByMeta(p, "role", "dimension");
//            List<String> dims2d = DataIO.keysForBounds(p, 2);
//            dimCombo.removeAllItems();
//            dimCombo.addItem("---- GENERAL ----");
//            dimCombo.addItem(" <ALL>");
//            dimCombo.addItem(" <SCALARS>");
//            dimCombo.addItem("------ DIMS -----");
//            for (String s : dims) {
//                dimCombo.addItem("  " + s);
//            }
//            dimCombo.addItem("------- 2D -------");
//            for (String s : dims2d) {
//                dimCombo.addItem("  " + s);
//            }
//            dimCombo.setSelectedIndex(1);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//    }
//
//    static class RowHeaderRenderer extends JLabel implements ListCellRenderer {
//
//        private static final long serialVersionUID = 1L;
//
//        RowHeaderRenderer(JTable table) {
//            JTableHeader header = table.getTableHeader();
//            setOpaque(true);
//            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
//            setHorizontalAlignment(CENTER);
//            setForeground(header.getForeground());
//            setBackground(header.getBackground());
//            setFont(header.getFont());
//        }
//
//        @Override
//        public Component getListCellRendererComponent(JList list,
//                Object value, int index, boolean isSelected, boolean cellHasFocus) {
//            setText((value == null) ? "" : value.toString());
//            return this;
//        }
//    }
//
//    /**
//     * The cell renderer.
//     */
//    static class MyCellRenderer extends DefaultTableCellRenderer {
//
//        private static final long serialVersionUID = 1L;
//        private static Color whiteColor = new Color(254, 254, 254);
//        private static Color alternateColor = new Color(244, 244, 244);
//        private static Color selectedColor = new Color(193, 210, 238);
//
//        @Override
//        public Component getTableCellRendererComponent(JTable table,
//                Object value, boolean selected, boolean focused,
//                int row, int column) {
//            super.getTableCellRendererComponent(table, value,
//                    selected, focused, row, column);
//
//            Color bg;
//            if (!selected) {
//                bg = (row % 2 == 0 ? alternateColor : whiteColor);
//            } else {
//                bg = selectedColor;
//            }
//            setBackground(bg);
//            setForeground(Color.black);
//            return this;
//        }
//    }
//
//    /** This method is called from within the constructor to
//     * initialize the form.
//     * WARNING: Do NOT modify this code. The content of this method is
//     * always regenerated by the Form Editor.
//     */
//    @SuppressWarnings("unchecked")
//    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
//    private void initComponents() {
//
//        jPanel2 = new javax.swing.JPanel();
//        jLabel6 = new javax.swing.JLabel();
//        jLabel9 = new javax.swing.JLabel();
//        split = new javax.swing.JSplitPane();
//        jToolBar1 = new javax.swing.JToolBar();
//        saveButton = new javax.swing.JButton();
//        jSeparator1 = new javax.swing.JToolBar.Separator();
//        jLabel2 = new javax.swing.JLabel();
//        dimCombo = new javax.swing.JComboBox();
//        jLabel1 = new javax.swing.JLabel();
//        fileCombo = new javax.swing.JComboBox();
//        jPanel1 = new javax.swing.JPanel();
//        numLabel = new javax.swing.JLabel();
//
//        jLabel6.setText("jLabel6");
//
//        jLabel9.setText("jLabel9");
//
//        setLayout(new java.awt.BorderLayout());
//
//        split.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
//        add(split, java.awt.BorderLayout.CENTER);
//
//        jToolBar1.setFloatable(false);
//        jToolBar1.setRollover(true);
//
//        saveButton.setText("save");
//        saveButton.setFocusable(false);
//        saveButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
//        saveButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
//        jToolBar1.add(saveButton);
//        jToolBar1.add(jSeparator1);
//
//        jLabel2.setText("  Filter: ");
//        jToolBar1.add(jLabel2);
//
//        jToolBar1.add(dimCombo);
//
//        jLabel1.setText("     File: ");
//        jToolBar1.add(jLabel1);
//
//        jToolBar1.add(fileCombo);
//
//        add(jToolBar1, java.awt.BorderLayout.PAGE_START);
//
//        numLabel.setText(" ");
//
//        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
//        jPanel1.setLayout(jPanel1Layout);
//        jPanel1Layout.setHorizontalGroup(
//            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
//            .addGroup(jPanel1Layout.createSequentialGroup()
//                .addComponent(numLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 308, Short.MAX_VALUE)
//                .addContainerGap())
//        );
//        jPanel1Layout.setVerticalGroup(
//            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
//            .addComponent(numLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
//        );
//
//        add(jPanel1, java.awt.BorderLayout.PAGE_END);
//    }// </editor-fold>//GEN-END:initComponents
//    // Variables declaration - do not modify//GEN-BEGIN:variables
//    private javax.swing.JComboBox dimCombo;
//    private javax.swing.JComboBox fileCombo;
//    private javax.swing.JLabel jLabel1;
//    private javax.swing.JLabel jLabel2;
//    private javax.swing.JLabel jLabel6;
//    private javax.swing.JLabel jLabel9;
//    private javax.swing.JPanel jPanel1;
//    private javax.swing.JPanel jPanel2;
//    private javax.swing.JToolBar.Separator jSeparator1;
//    private javax.swing.JToolBar jToolBar1;
//    private javax.swing.JLabel numLabel;
//    private javax.swing.JButton saveButton;
//    private javax.swing.JSplitPane split;
//    // End of variables declaration//GEN-END:variables
//
//    public static JFrame create(
//            File[] csv) throws Exception {
//        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//
//        final PEditor p = new PEditor(csv);
//        JFrame f = new JFrame();
//        f.getContentPane().add(p);
//        f.setTitle("Parameter Editor v3.0");
//        f.setIconImage(
//                new ImageIcon(PEditor.class.getResource("/ngmf/ui/table.png")).getImage());
//        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        f.setSize(800, 600);
//        f.setLocation(500, 200);
//        f.setVisible(true);
//        f.toFront();
//        f.addWindowListener(new WindowAdapter() {
//
//            @Override
//            public void windowOpened(
//                    WindowEvent e) {
//                p.cp.requestFocus();
//            }
//        });
//        return f;
//    }
//
//    public static void main(String[] args) throws Exception {
//        System.setProperty("work", "c:/tmp");
////        File file = new File("C:/Users/brandondaniel/Desktop/Conversion/oms3.prj.prms2008/data/efc_svntest3.csv");
//        File file = new File("C:/od/projects/oms3.prj.prms2008/data/efc_svntest3_1.csv");
////        File file1 = new File("C:/tmp/efc1.csp");
//        create(new File[]{file});
//    }
//}
