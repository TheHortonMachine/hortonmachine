///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package oms3;
//
//import oms3.dsl.Buildable;
//import oms3.dsl.GenericBuilderSupport;
//
///**
// * SimBuilder class for all oms simulation DSLs
// *
// * @author od
// */
//public class SimBuilder extends GenericBuilderSupport {
//
//    @Override
//    public  Class<? extends Buildable> lookupTopLevel(Object n1) {
//        String cl = null;
//        String name = n1.toString();
//        if (name.equals("sim")) {
//            cl = "oms3.dsl.Sim";
//        } else if (name.equals("esp")) {
//            cl = "oms3.dsl.esp.Esp";
//        } else if (name.equals("luca")) {
//            cl = "oms3.dsl.cosu.Luca";
//        } else if (name.equals("fast")) {
//            cl = "oms3.dsl.cosu.Fast";
//        } else if (name.equals("dds")) {
//            cl = "oms3.dsl.cosu.DDS";
//        } else if (name.equals("glue")) {
//            cl = "oms3.dsl.cosu.Glue";
//        } else if (name.equals("tests")) {
//            cl = "oms3.dsl.Tests";
//        } else if (name.equals("chart")) {
//            cl = "oms3.dsl.analysis.Chart";
//        } else {
//            throw new ComponentException("unknown element '" + name + "'");
//        }
//        
//        try {
//            return (Class<? extends Buildable>) Class.forName(cl);
//        } catch (ClassNotFoundException ex) {
//            throw new Error("DSL Not found '" + ex.getMessage() + "'");
//        }
//    }
//}
