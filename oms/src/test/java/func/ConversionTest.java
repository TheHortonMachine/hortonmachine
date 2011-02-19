/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package func;

import java.math.BigDecimal;
import oms3.Conversions;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Olaf David
 */
public class ConversionTest {

    static boolean debug = false;

    @Test
    public void testArrayParser() {
        String[] arr = Conversions.parseArrayElement("test[1]");
        Assert.assertNotNull(arr);
        Assert.assertTrue(arr.length == 2);
        Assert.assertEquals("test", arr[0]);
        Assert.assertEquals("1", arr[1]);

        arr = Conversions.parseArrayElement("test");
        Assert.assertNotNull(arr);
        Assert.assertTrue(arr.length == 1);
        Assert.assertEquals("test", arr[0]);

        arr = Conversions.parseArrayElement("test[1][24][3]");
        Assert.assertNotNull(arr);
        Assert.assertTrue(arr.length == 4);
        Assert.assertEquals("test", arr[0]);
        Assert.assertEquals("1", arr[1]);
        Assert.assertEquals("24", arr[2]);
        Assert.assertEquals("3", arr[3]);

        arr = Conversions.parseArrayElement("test[]");
        Assert.assertNotNull(arr);
        Assert.assertTrue(arr.length == 1);
        Assert.assertEquals("test", arr[0]);
    }
    
    @Test
    public void testEmptyArray() {
        Conversions.debug = debug;
        double[] arr = Conversions.convert("{}", double[].class);
        Assert.assertTrue(arr != null && arr.length == 0);
        arr = Conversions.convert(" { } ", double[].class);
        Assert.assertTrue(arr != null && arr.length == 0);
        arr = Conversions.convert(" {} ", double[].class);
        Assert.assertTrue(arr != null && arr.length == 0);
        arr = Conversions.convert(" {} \n", double[].class);
        Assert.assertTrue(arr != null && arr.length == 0);
    }

    @Test
    public void test1DOneElementArray() {
        Conversions.debug = debug;
        String content = " {  1.34 } ";
        double[] a = Conversions.convert(content, double[].class);
        Assert.assertNotNull(a);
        Assert.assertTrue(a.length == 1);
        Assert.assertEquals(1.34, a[0], 0.0d);
    }

    @Test
    public void test1DArray() {
        Conversions.debug = debug;
        String content = "{3.3, 1.1, 4.1, 2.4, 1.2, 3.4}";
        double[] a = Conversions.convert(content, double[].class);

        Assert.assertNotNull(a);
        Assert.assertTrue(a.length == 6);
        Assert.assertEquals(3.3, a[0], 0.0d);
        Assert.assertEquals(1.1, a[1], 0.0d);
        Assert.assertEquals(4.1, a[2], 0.0d);
        Assert.assertEquals(2.4, a[3], 0.0d);
        Assert.assertEquals(1.2, a[4], 0.0d);
        Assert.assertEquals(3.4, a[5], 0.0d);
    }

    @Test
    public void test2DArray() {
        Conversions.debug = debug;
        String content = "{{3.3, 1.2, 4.1} ,  { 2.4 , 1.2, 3.4}}";
        double[][] a = Conversions.convert(content, double[][].class);

        Assert.assertNotNull(a);

        Assert.assertTrue(a.length == 2);
        Assert.assertTrue(a[0].length == 3);
        Assert.assertTrue(a[1].length == 3);

        Assert.assertEquals(3.3, a[0][0], 0.0d);
        Assert.assertEquals(1.2, a[0][1], 0.0d);
        Assert.assertEquals(4.1, a[0][2], 0.0d);
        Assert.assertEquals(2.4, a[1][0], 0.0d);
        Assert.assertEquals(1.2, a[1][1], 0.0d);
        Assert.assertEquals(3.4, a[1][2], 0.0d);
    }

    @Test
    public void test3DArray() {
        Conversions.debug = debug;
        String content = "{{{3.3, 1.2} ,{4.1, 2.4},  {1.2, 3.4}}}";
        double[][][] a = Conversions.convert(content, double[][][].class);

        Assert.assertNotNull(a);

        Assert.assertTrue(a.length == 1);
        Assert.assertTrue(a[0].length == 3);
        Assert.assertTrue(a[0][0].length == 2);
        Assert.assertTrue(a[0][1].length == 2);
        Assert.assertTrue(a[0][2].length == 2);

        Assert.assertEquals(3.3, a[0][0][0], 0.0d);
        Assert.assertEquals(1.2, a[0][0][1], 0.0d);
        Assert.assertEquals(4.1, a[0][1][0], 0.0d);
        Assert.assertEquals(2.4, a[0][1][1], 0.0d);
        Assert.assertEquals(1.2, a[0][2][0], 0.0d);
        Assert.assertEquals(3.4, a[0][2][1], 0.0d);
    }

    @Test
    public void testBigDecimal() {
        String content = "2.34";
        BigDecimal bd = Conversions.convert(content, BigDecimal.class);
        Assert.assertEquals(new BigDecimal("2.34"), bd);
    }

    @Test
    public void testdouble() {
        String content = "2.34";
        double bd = Conversions.convert(content, double.class);
        Assert.assertEquals(2.34, bd, 0.00000001);
    }

    @Test
    public void testDouble() {
        String content = "2.34";
        double bd = Conversions.convert(content, Double.class);
        Assert.assertEquals(2.34, bd, 0.00000001);

        Double bd1 = Conversions.convert(content, Double.class);
        Assert.assertEquals(2.34, bd1, 0.00000001);
    }
}
