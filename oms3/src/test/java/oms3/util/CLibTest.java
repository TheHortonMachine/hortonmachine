/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author od
 */
public class CLibTest {

    static CTestLib lib;

    interface CTestLib extends Library {
        int add(int p, int y);
        int addarr(int[] arr, int len);
        int stringpass(String str);
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Native.setProtected(true);
//        System.setProperty("jna.library.path", "c:/od/projects/CDynamicLibrary/dist/Debug/MinGW-Windows/");
        System.setProperty("jna.library.path", "/od/projects/CppDLL/dist/Debug/GNU-Linux-x86/");
//        lib = (CTestLib) Native.loadLibrary("CDyn", CTestLib.class);
        lib = (CTestLib) Native.loadLibrary("CppDLL", CTestLib.class);
    }

    @Test
    public void add() {
        int sum = lib.add(1, 2);
        assertEquals(3, sum);
    }

    @Test
    public void strpass() {
        int result = lib.stringpass("str_test");
        assertEquals(0, result);
    }

    @Test
    public void addarr() {
        int[] a = {1, 2, 3, 4, 5};
        lib.addarr(a, a.length);
        assertArrayEquals(new int[]{4, 5, 6, 7, 8}, a);
    }
}
