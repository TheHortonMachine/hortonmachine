/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.ptr.ByReference;
import com.sun.jna.ptr.IntByReference;
import oms3.annotations.DLL;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author od
 */
public class FLibTest {

    static F95Test lib;

    public static class City extends Structure {

        public City(int Population, double Latitude, double Longitude, int Elevation) {
            this.Population = Population;
            this.Latitude = Latitude;
            this.Longitude = Longitude;
            this.Elevation = Elevation;
        }

        public City() {
        }
        
        public int Population;
        public double Latitude,  Longitude;
        public int Elevation;
    }

//    @DLL("F90Dyn")
    @DLL("F90DLL")
    interface F95Test extends Library {

        int foomult(int a, int b);
        double fooaddreal(double a, double b);
        void fooinc(int[] arr, int len);
//        void arr2d(int[] arr, int m, int n);
//        boolean foostr(String s, int len);
        boolean  __test_MOD_strpass(String s, int len);
//
//        // Module name + _MP_ + function name, there is no alias
//        void test_MP_ffunc(ByReference a, ByReference b);
//        void footype(City c);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Native.setProtected(true);
//        System.setProperty("jna.library.path", "c:/od/projects/F90DynamicLibrary/dist/Debug/GNU-Windows");
        System.setProperty("jna.library.path", "/od/projects/F90DLL/dist/Debug/GNU-Linux-x86");
//        NativeLibrary.addSearchPath("F90Dyn", "c:/od/projects/F90DynamicLibrary/dist/Debug/GNU-Windows");
//        lib = (F95Test) Native.loadLibrary("F90Dyn", F95Test.class);
        lib = NativeLibraries.bind(F95Test.class);
    }

    @Test
    public void foomult() {
        int r = lib.foomult(2, 2);
        assertEquals(4, r);
    }

    @Test
    public void fooaddreal() {
        double r = lib.fooaddreal(6.0d, 3.0d);
        assertEquals(18, r, 0.0001);
    }
//
//    @Test
//    public void foofunc() {
//        IntByReference a = new IntByReference(0);
//        IntByReference b = new IntByReference(0);
//        lib.test_MP_ffunc(a, b);
//        assertEquals(3, a.getValue());
//        assertEquals(5, b.getValue());
//    }
//
    @Test
    public void strpass() {
        String test = "str_testkakakakakak";
        boolean result = lib.__test_MOD_strpass(test, test.length());
        assertTrue(result);
    }
//
//    @Test
//    public void arr2d() {
//        int[] a = {1, 2, 3, 4, 5, 6};
//        lib.arr2d(a, 3, 2);
//        assertArrayEquals(new int[]{2, 3, 4, 5, 6, 7}, a);
//    }
//
//    @Test
//    public void footypedef() {
//        City city = new City(3000, 0.222, 0.333, 1001);
//        lib.footype(city);
//
//        assertEquals(4000, city.Population);
//        assertEquals(5.222, city.Latitude, 0.0001);
//        assertEquals(5.333, city.Longitude, 0.0001);
//        assertEquals(1010, city.Elevation);
//    }
//
    @Test
    public void fooinc() {
        int[] a = {1, 2, 3, 4, 5};
        lib.fooinc(a, a.length);
        assertArrayEquals(new int[]{31, 32, 33, 34, 35}, a);
    }
}