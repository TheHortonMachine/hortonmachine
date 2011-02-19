/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.io;

import oms3.io.CSTable;
import oms3.io.DataIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Olaf David
 */
public class UtilsTest {

    File r;

    @Before
    public void init() throws FileNotFoundException {
        r = new File(this.getClass().getResource("test.csv").getFile());
    }

    @After
    public void done() throws IOException {
    }

    @Test
    public void testTableInfo() throws Exception {
//        CSTable t = DataIO.table(r, "Olaf");
//        Assert.assertNotNull(t);
//        DataIO.print(t, new PrintWriter(System.out));
    }//    @Test
//    public void test() throws Exception {
//       Utils.convert("Parameter", new FileReader("c:/oms/work21/prms/data/efcarson.pps"),
//               new PrintStream("c:/oms/work21/prms/data/efcarson.csproperties"));
//       Utils.convert("Parameter", new FileReader("c:/oms/work21/prms/data/efcarson_files.pps"),
//               new PrintStream("c:/oms/work21/prms/data/efcarson_files.csproperties"));
//       Utils.convert("Parameter", new FileReader("c:/oms/work21/prms/data/efcarson_dates.pps"),
//               new PrintStream("c:/oms/work21/prms/data/efcarson_dates.csproperties"));
//    }
//
////    @Test
//    public void testRowLookup() throws Exception {
//        CSTable t = DataIO.table(r, "EFCarson");
//        Assert.assertNotNull(t);
//        Calendar c = new GregorianCalendar(1980, 9, 1);
//        int row = Utils.findRowByDate(c.getTime(), 0, new MemoryTable(t));
//        System.out.println("row " + row);
////        Utils.print(t, System.out);
//    }
//
//    // @Test
//    public void testESPLookup() throws Exception {
//        CSTable t = DataIO.table(r, "EFCarson");
//        Assert.assertNotNull(t);
//        Date iniStart = new GregorianCalendar(1986, 07, 10).getTime();
//        Date iniEnd = new GregorianCalendar(1986, 07, 20).getTime();
//
//        CSTable esp = Utils.synthESPInput(t, iniStart, iniEnd, 5, 1981);
//        Utils.print(esp, new PrintWriter(System.out));
//    }
}
