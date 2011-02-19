/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.io;

import oms3.io.CSTable;
import oms3.io.TableIterator;
import oms3.io.DataIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Olaf David
 */
public class TableTest {

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
        CSTable t = DataIO.table(r, "Olaf");
        Assert.assertNotNull(t);

        Assert.assertEquals("Olaf", t.getName());
        Assert.assertEquals("temp", t.getColumnName(1));
        Assert.assertEquals("precip", t.getColumnName(2));

        Assert.assertEquals("today", t.getInfo().get("created"));
        Assert.assertEquals("this smsmss.", t.getInfo().get("description"));
        Assert.assertEquals(2, t.getInfo().size());

        Assert.assertEquals("F", t.getColumnInfo(1).get("unit"));
        Assert.assertEquals(1, t.getColumnInfo(1).size());
        Assert.assertEquals("mm", t.getColumnInfo(2).get("unit"));
        Assert.assertEquals(1, t.getColumnInfo(2).size());
    }

    @Test
    public void testTableLayout() throws Exception {
        CSTable t = DataIO.table(r, "Olaf");
        Assert.assertNotNull(t);
        Assert.assertEquals(2, t.getColumnCount());
        int rows = 0;
        for (String[] row : t.rows()) {
            rows++;
            Assert.assertEquals(3, row.length);
        }
        Assert.assertEquals(5, rows);
    }

    @Test
    public void testFirstTable() throws Exception {
        CSTable t = DataIO.table(r, null);
        Assert.assertNotNull(t);
        Assert.assertEquals(t.getName(), "Olaf");
    }

    @Test
    public void testTableData() throws Exception {
        CSTable t = DataIO.table(r, "Olaf");
        Assert.assertNotNull(t);
        Iterator<String[]> rows = t.rows().iterator();
        Assert.assertNotNull(rows);
        Assert.assertTrue(rows.hasNext());
        Assert.assertArrayEquals(new String[]{"1", "2.4", "3.5"}, rows.next());
        Assert.assertTrue(rows.hasNext());
        Assert.assertArrayEquals(new String[]{"2", "2.4", "2.5"}, rows.next());
        Assert.assertTrue(rows.hasNext());
        Assert.assertArrayEquals(new String[]{"3", "4.7", "4.1"}, rows.next());
        Assert.assertTrue(rows.hasNext());
    }

    @Test
    public void testTableDataOffset() throws Exception {
        CSTable t = DataIO.table(r, "Olaf");
        Assert.assertNotNull(t);
        Iterator<String[]> rows = t.rows(2).iterator();
        Assert.assertNotNull(rows);
        Assert.assertTrue(rows.hasNext());
        Assert.assertArrayEquals(new String[]{"3", "4.7", "4.1"}, rows.next());
        Assert.assertTrue(rows.hasNext());
    }

    @Test
    public void testtwoTables() throws Exception {
        CSTable t = DataIO.table(r, "Olaf");
        Assert.assertNotNull(t);
        Iterator<String[]> rows = t.rows().iterator();
        Assert.assertNotNull(rows);
        Assert.assertTrue(rows.hasNext());
        Assert.assertArrayEquals(new String[]{"1", "2.4", "3.5"}, rows.next());
        Assert.assertTrue(rows.hasNext());
        Assert.assertArrayEquals(new String[]{"2", "2.4", "2.5"}, rows.next());
        Assert.assertTrue(rows.hasNext());
        Assert.assertArrayEquals(new String[]{"3", "4.7", "4.1"}, rows.next());
        Assert.assertTrue(rows.hasNext());

        Iterator<String[]> rows1 = t.rows().iterator();
        Assert.assertNotNull(rows1);
        Assert.assertTrue(rows1.hasNext());
        Assert.assertArrayEquals(new String[]{"1", "2.4", "3.5"}, rows1.next());
        Assert.assertTrue(rows1.hasNext());
        Assert.assertArrayEquals(new String[]{"2", "2.4", "2.5"}, rows1.next());
        Assert.assertTrue(rows1.hasNext());
        Assert.assertArrayEquals(new String[]{"3", "4.7", "4.1"}, rows1.next());
        Assert.assertTrue(rows1.hasNext());
    }

    @Test
    public void testSkip() throws Exception {
        CSTable t = DataIO.table(r, "EFCarson");
        Assert.assertNotNull(t);

        Iterator<String[]> rows = t.rows().iterator();
        Assert.assertNotNull(rows);
        Assert.assertTrue(rows.hasNext());
        Assert.assertEquals("84.0", rows.next()[2]);
        Assert.assertTrue(rows.hasNext());
        ((TableIterator) rows).skip(5);
        Assert.assertEquals("78.0", rows.next()[2]);
        ((TableIterator) rows).skip(10);
        Assert.assertEquals("99.0", rows.next()[2]);
        ((TableIterator) rows).skip(1);
        Assert.assertEquals("96.0", rows.next()[2]);
        Assert.assertTrue(rows.hasNext());
    }

    @Test
    public void testTableCol() throws Exception {
        CSTable t = DataIO.table(r, "Olaf");
        Assert.assertNotNull(t);
        Double[] vals = DataIO.getColumnDoubleValues(t, "precip");
        Assert.assertEquals(5, vals.length);
        Assert.assertEquals(3.5, vals[0], 0);
        Assert.assertEquals(2.5, vals[1], 0);
        Assert.assertEquals(4.1, vals[2], 0);
        Assert.assertEquals(4.2, vals[3], 0);
        Assert.assertEquals(4.3, vals[4], 0);
    }


    @Test
    public void testCreateTable() {
        MemoryTable mt = new MemoryTable();
        mt.setName("MyTable");
        mt.getInfo().put("table_metadata", "here");
        mt.getInfo().put("table_metadata1", "here_too");
        mt.setColumns(new String[] { "c1", "c2", "c3", "c4"});
        mt.getColumnInfo(1).put("unit", "C");
        mt.getColumnInfo(2).put("unit", "F");
        mt.getColumnInfo(3).put("unit", "");
        mt.getColumnInfo(4).put("unit", "");
        
        mt.addRow(new Object[] { "1", "2","3", "4"});
        mt.addRow(new Object[] { "1", "2","3", "4"});
        mt.addRow(new Object[] { "1", "2","3", "4"});
        mt.addRow("1", "2","3", "4v");
        
        DataIO.print(mt, new PrintWriter(System.out));
    }

//    @Test
    public void testStations() throws Exception {

        File climateInput = new File("c:/tmp/clim_data_test.csv");
        int staid;
        String staname;
        String state;
        double lng;
        double lat;
        int elev;

        CSTable table = DataIO.table(climateInput, "Data for met test");
        Iterator<String[]> inp = table.rows().iterator();
        while (inp.hasNext()) {
            String[] row = inp.next();
            System.out.println(Arrays.toString(row));
            staid = Integer.parseInt(row[0]);
            staname = row[1];
            state = row[2];
            lng = Double.parseDouble(row[3]);
            lat = Double.parseDouble(row[4]);
            elev = Integer.parseInt(row[5]);
        }
    }

//    public static void main(String[] args) throws Exception {
//        Reader r = new FileReader(TableTest.class.getResource("test.csv").getFile());
//        Table t = DataIO.table(r, "Olaf");
//        TableModel m = Utils.tableModel(t);
//        JFrame f = new JFrame();
//        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        f.getContentPane().setLayout(new BorderLayout());
//        f.getContentPane().add(new JScrollPane(new JTable(m)), BorderLayout.CENTER);
//        f.pack();
//        f.setSize(300, 200);
//        f.setVisible(true);
//    }
}
