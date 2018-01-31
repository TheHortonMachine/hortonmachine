//package oms3.dsl.analysis;
//
//import ngmf.ui.graph.ValueSet;
//import oms3.dsl.*;
//import java.awt.BorderLayout;
//import java.util.ArrayList;
//import java.util.List;
//import javax.swing.ImageIcon;
//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.JTabbedPane;
//import ngmf.ui.graph.PlotView;
//import ngmf.util.OutputStragegy;
//import oms3.SimConst;
//import org.omscentral.modules.analysis.esp.ESPToolPanel;
//
//public class Chart implements Buildable {
//
//    String title = "Chart";
//    List<Buildable> plots = new ArrayList<Buildable>();
//
//    public String getTitle() {
//        return title;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    @Override
//    public Buildable create(Object name, Object value) {
//        if (name.equals("timeseries")) {
//            Plot p = new Plot();
//            plots.add(p);
//            return p;
//        } else if (name.equals("flowduration")) {
//            FlowDur f = new FlowDur();
//            plots.add(f);
//            return f;
//        } else if (name.equals("scatter")) {
//            Scatter f = new Scatter();
//            plots.add(f);
//            return f;
//        } else if (name.equals("esptraces")) {
//            EspTrace f = new EspTrace();
//            plots.add(f);
//            return f;
//        }
//        throw new IllegalArgumentException(name.toString());
//    }
//
//    public void run(OutputStragegy st, String name) {
//        try {
//            JPanel panel = new JPanel(new BorderLayout());
//            JTabbedPane tabs = new JTabbedPane();
//            panel.add(tabs, BorderLayout.CENTER);
//            for (Buildable b : plots) {
//                if (b instanceof Plot) {
//                    Plot p = (Plot) b;
//                    List<String> names = new ArrayList<String>();
//                    for (ValueSet axis : p.getY()) {
//                        names.add(axis.getName());
//                    }
//                    List<Double[]> vals = new ArrayList<Double[]>();
//                    for (ValueSet axis : p.getY()) {
//                        vals.add(axis.getDoubles(st.baseFolder(), name));
//                    }
//                    String view = p.getView();
//                    int type = 1;
//                    if (view.equals(SimConst.STACKED)) {
//                        type = 0;
//                    } else if (view.equals(SimConst.MULTI)) {
//                        type = 1;
//                    } else if (view.equals(SimConst.COMBINED)) {
//                        type = 2;
//                    }
//                    tabs.addTab(p.getTitle(), PlotView.createTSChart(p.getTitle(), p.getX().
//                            getDates(st.baseFolder(), name), names, vals, type, p.getY()));
////                    tabs.addTab(p.getTitle(), PlotView.createTSChart(p.getTitle(), p.getX().getDates(st.baseFolder(), name), names, vals, type));
//                } else if (b instanceof FlowDur) {
//                    FlowDur p = (FlowDur) b;
//                    List<String> names = new ArrayList<String>();
//                    for (ValueSet axis : p.getY()) {
//                        names.add(axis.getName());
//                    }
//                    List<Double[]> vals = new ArrayList<Double[]>();
//                    for (ValueSet axis : p.getY()) {
//                        vals.add(axis.getDoubles(st.baseFolder(), name));
//                    }
//                    Integer[] i = new Integer[100];
//                    for (int j = 0; j < i.length; j++) {
//                        i[j] = j;
//                    }
//                    tabs.addTab(p.getTitle(), PlotView.createLineChart(p.getTitle(), i, names, vals));
//                } else if (b instanceof Scatter) {
//                    Scatter p = (Scatter) b;
//                    tabs.addTab(p.getTitle(), PlotView.createScatterChart(p.getTitle(),
//                            p.getX().getName(), p.getY().getName(),
//                            p.getX().getDoubles(st.baseFolder(), name), p.getY().getDoubles(st.baseFolder(), name)));
//                } else if (b instanceof EspTrace) {
//                    EspTrace p = (EspTrace) b;
//                    ESPToolPanel pa = PlotView.createESPTraces(p.getTitle(), p.getDir(st), p.getVar());
//                    String report = p.getReport(st);
//                    if (report != null) {
//                        pa.writeReport(report);
//                    }
//                    tabs.addTab(p.getTitle(), pa);
//                }
//            }
//
//            if (tabs.getTabCount() == 0) {
//                return;
//            }
//            JFrame f = new JFrame(getTitle() + "  [" + st.lastOutputFolder() + "]");
//            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            f.getContentPane().add(panel);
//            f.setIconImage(new ImageIcon(PlotView.class.getResource("/ngmf/ui/graph/bar-chart-16x16.png")).getImage());
////            f.pack();
//            f.setSize(800, 600);
//            f.setLocation(500, 300);
//            f.setVisible(true);
//            f.toFront();
//        } catch (Exception E) {
//            E.printStackTrace(System.out);
//        }
//    }
//}
