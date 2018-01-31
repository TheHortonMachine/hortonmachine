///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package oms3.util;
//
//import com.sun.jna.Library;
//import com.sun.jna.Native;
//import oms3.annotations.DLL;
//
///**
// * Simplifies runtime binding of dynamic shared libraries to Java Interfaces.
// * 
// * @author od
// */
//public class NativeLibraries {
//
//    private NativeLibraries() {
//    }
//
//    /**
//     * Maps a DLL to an interface using the @DLL annotation.
//     *
//     * @param <T> The interface type
//     * @param intf  The interface class
//     * @return the Library instance of type T
//     *
//     * @see oms3.annotations.DLL
//     */
//    public static <T extends Library> T bind(Class<T> intf) {
//        DLL dll = intf.getAnnotation(DLL.class);
//        if (dll == null) {
//            throw new IllegalArgumentException("Missing DLL annotation.");
//        }
//        return bind(dll.value(), intf);
//    }
//
//    /**
//     * Bind the Library 'name' to the interface 'intf', No annotation needed.
//     * 
//     * @param <T>
//     * @param name the library name, if not provided the Interface
//     *             must be annotated with '@DLL'
//     * @param intf the interface
//     * @return The library interface bound to a DLL.
//     */
//    @SuppressWarnings("unchecked")
//    public static <T extends Library> T bind(String name, Class<T> intf) {
//
//        if (!intf.isInterface()) {
//            throw new IllegalArgumentException("Expected interface, but got '" + intf + "'.");
//        }
//        if (name.isEmpty()) {
//            throw new IllegalArgumentException("Empty library name.");
//        }
//        Native.setProtected(true);
//        return (T) Native.loadLibrary(name, intf);
//    }
//}
