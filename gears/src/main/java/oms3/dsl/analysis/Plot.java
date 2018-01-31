//package oms3.dsl.analysis;
//
//import oms3.dsl.*;
//import oms3.ngmf.ui.graph.ValueSet;
//
//import java.util.ArrayList;
//import java.util.List;
//import oms3.SimConst;
//
///**
// * 
// * @author od
// */
//public class Plot implements Buildable {
//
//    Axis x;
//    List<ValueSet> y = new ArrayList<ValueSet>();
//    String title;
//    String view = SimConst.MULTI;
//    
//    public void setView(String view) {
//        this.view = view;
//    }
//
//    public String getView() {
//        return view;
//    }
//
//    public String getTitle() {
//        return title;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    Axis getX() {
//        return x;
//    }
//
//    List<ValueSet> getY() {
//        return y;
//    }
//
//    @Override
//    public Buildable create(Object name, Object value) {
//        if (name.equals("x")) {
//            x = new Axis();
//            return x;
//        } else if (name.equals("y")) {
//            Axis a = new Axis();
//            y.add(a);
//            return a;
//        } else if (name.equals("calc")) {
//            Calc a = new Calc();
//            y.add(a);
//            return a;
//        }
//        throw new IllegalArgumentException("plot cannot handle :" + name);
//    }
//}
