package org.jgrasstools.gears.spatialite;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import org.sqlite.SQLiteConfig;

public class SpatialiteThreads {
    // max concurrent threads supported by Spatialite
    public static final int MAX_THREADS = 64;

    private static class myThread extends Thread {
        //
        // the (private) Class implementing each child Thread
        //
        int slot;
        int thread_no;

        public myThread( int slot ) {
            // ctor
            this.slot = slot;
        }

        public void setThreadNo( int thread_no ) {
            // setting the Thread counter
            this.thread_no = thread_no;
        }

        public void run() {
            // actual Thread implementation
            // all real work happens here
            boolean ok1 = false;
            boolean ok2 = false;
            Connection conn = null;

            // welcome message
            System.out.println("start: Slot #" + slot + "    Thread #" + thread_no);

            try {
                // enabling dynamic extension loading
                // absolutely required by SpatiaLite
                SQLiteConfig config = new SQLiteConfig();
                config.enableLoadExtension(true);

                // create a database connection
                conn = DriverManager.getConnection("jdbc:sqlite:" + SpatialiteSample.dbPath, config.toProperties());
                Statement stmt = conn.createStatement();
                stmt.setQueryTimeout(30); // set timeout to 30 sec.

                // loading SpatiaLite
                stmt.execute("SELECT load_extension('mod_spatialite')");

                // preparing the SQL query statement
                String sql = "";
                // switch (thread_no % 3) {
                // // each Thread will select its own query between three
                // // possible alternative depending on thread-counter value
                // case 0:
//                sql = "SELECT * FROM test_pt WHERE ";
//                sql += "ST_Intersects(geom, BuildMbr(10.23, 10.23, 10.25, 10.25)) = 1";
                // break;
                // case 1:
                 sql = "SELECT * FROM test_ln WHERE ";
                 sql += "ST_Intersects(geom, BuildMbr(40.09, 30.09, 40.1, 30.1)) = 1";
                // break;
                // case 2:
                // sql = "SELECT Sum(ST_Area(geom)), Sum(ST_Perimeter(geom)) ";
                // sql += "FROM test_pg WHERE id BETWEEN 10000 AND 10005";
                // break;
                // }

                // creating a ResultSet
                ResultSet rs = stmt.executeQuery(sql);
                while( rs.next() ) {
                    // reading the result set
                    String name;
                    double value;
                    // switch (thread_no % 3) {
                    // case 0:
                    // name = rs.getString(2);
                    // if (name.equals("test POINT #10241")) {
                    // ok1 = true;
                    // }
                    // if (name.equals("test POINT #10249")) {
                    // ok2 = true;
                    // }
                    // break;
                    // case 1:
                    name = rs.getString(2);
//                    System.out.println("NAME:" + name);
                    if (name.equals("test LINESTRING #12606")) {
                        ok1 = true;
                    }
                    if (name.equals("test LINESTRING #12618")) {
                        ok2 = true;
                    }
                    // break;
                    // case 2:
                    // value = rs.getDouble(1);
                    // if (value > 9601.440123 && value < 9601.440125) {
                    // ok1 = true;
                    // }
                    // value = rs.getDouble(2);
                    // if (value > 960.071999 && value < 960.072001) {
                    // ok2 = true;
                    // }
                    // break;
                    // }
                }

            } catch (SQLException e) {
                // if the error message is "out of memory",
                // it probably means no database file is found
                e.printStackTrace();;
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (SQLException e) {
                    // connection close failed.
                    e.printStackTrace();
                }
            }

            // goodbye message
            if (ok1 == true && ok2 == true) {
                System.out.println("    stop: Slot #" + slot + "    Thread #" + thread_no);
            } else {
                System.out.println("    ***** ERROR ***** stop: Slot #" + slot + "    Thread #" + thread_no);
            }
            // thread termination: quitting
        }
    }

    public static void main( String[] args ) throws ClassNotFoundException {
        //
        // the Main Class is just intended to dispatch all children Threads
        //
        System.setProperty("java.io.tmpdir", "D:/TMP/");
        int thread_no = 0;
        int i;
        int slot;

        // load the sqlite-JDBC driver using the current class loader
        Class.forName("org.sqlite.JDBC");

        // creating and initializing all children threads
        myThread thread_array[] = new myThread[MAX_THREADS];
        for( slot = 0; slot < MAX_THREADS; slot++ ) {
            thread_array[slot] = new myThread(slot);
        }

        while( thread_no < 1000 ) {
            // looping on threads activation
            for( slot = 0; slot < MAX_THREADS; slot++ ) {
                // scanning all threads one by one
                if (thread_array[slot].isAlive() != true) {
                    // found a free slot:
                    // - this thred wasn't yet previously executed
                    // or
                    // - this thread was already executed and is now terminated
                    if (thread_no >= MAX_THREADS) {
                        // Java forbids to restart yet again a terminated thread
                        // so we'll now create a fresh thread on the same slot
                        thread_array[slot] = new myThread(slot);
                    }
                    thread_array[slot].setThreadNo(thread_no++);
                    // starting thread execution
                    thread_array[slot].start();
                }
            }
        }
    }
}