/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.io;

import oms3.io.CSProperties;
import oms3.io.CSVStrategy;
import oms3.io.CSVParser;
import oms3.io.DataIO;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Olaf David
 */
public class PropertiesTest {

    Reader r;

    @Before
    public void init() throws FileNotFoundException {
        r = new FileReader(this.getClass().getResource("test.csv").getFile());
    }

    private Reader[] open(String... f) throws FileNotFoundException {
        Reader[] r = new Reader[f.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = new FileReader(this.getClass().getResource(f[i]).getFile());
        }
        return r;
    }

    @After
    public void done() throws IOException {
        r.close();
    }

    //@Test
    public void hello() throws Exception {
        CSVParser reader = new CSVParser(new FileReader("c:/tmp/abc.csv"), CSVStrategy.DEFAULT_STRATEGY);
        String[] nextLine;
        while ((nextLine = reader.getLine()) != null) {
            for (String string : nextLine) {
                System.out.println("token " + string);
            }
            System.out.println("-----");
        }
    }

//    @Test
    public void test1() throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(this.getClass().getResource("test.csv").getFile()));
        CSVParser reader = new CSVParser(r, CSVStrategy.DEFAULT_STRATEGY);
        String[] nextLine;
        while ((nextLine = reader.getLine()) != null) {
            for (String string : nextLine) {
                System.out.println("token " + string);
            }
            System.out.println("-----");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWroneProp() throws Exception {
        CSProperties p = DataIO.properties(r, "notThere");
    }

    @Test
    public void testProp() throws Exception {
        CSProperties p = DataIO.properties(r, "hello");

        Assert.assertEquals("olaf", p.getInfo().get("by"));
        Assert.assertEquals("1.234", p.get("temp"));
        Assert.assertEquals("20", p.getInfo("temp").get("dim"));

        Assert.assertEquals(null, p.get("empty"));
        Assert.assertTrue(p.containsKey("empty"));
        Assert.assertEquals("1", p.getInfo("empty").get("index"));
        Assert.assertTrue(p.getInfo("empty").containsKey("public"));
        Assert.assertTrue(p.getInfo("empty").containsKey("required"));

        Assert.assertEquals(3, p.keySet().size());
        Assert.assertEquals(3, p.values().size());
    }

    @Test
    public void testPropSet2() throws Exception {
        CSProperties p = DataIO.properties(r, "set1");
        Assert.assertEquals(11, p.keySet().size());
        Assert.assertEquals(11, p.values().size());
    }

    @Test
    public void testPropSetMerge() throws Exception {
        Reader[] reader = open("proptest1.csp", "proptest2.csp");
        CSProperties p = DataIO.properties(reader, "parameter");

        Assert.assertEquals(5, p.keySet().size());
        Assert.assertEquals(5, p.values().size());
//        DataIO.print(p, new PrintWriter(System.out));
    }
}
