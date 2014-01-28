package org.jgrasstools.gears.io.las.core.liblas;

import java.io.File;

import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasReader;
import org.jgrasstools.gears.io.las.utils.LasUtils;

public class Test {

    public static void main( String[] args ) throws Exception {

        String las = "/home/moovida/data/geologico/LIDAR_Campolongo_tav_1059_1060/DATI_GREZZI/001059_1.las";
        // String las = "/media/lacntfs/geologico/las/fino_1400/001060_4.las";

        String lib = "/home/moovida/development/liblas-git/makefiles/bin/Release/";// liblas_c.so.2.2.0";
        LiblasReader.loadNativeLibrary(lib, "las_c");

        LiblasReader libLas = new LiblasReader(new File(las), null);
        libLas.open();

        LasReader lasReader = new LasReader(new File(las), null);
        lasReader.open();

        ILasHeader header1 = libLas.getHeader();
        System.out.println("points: " + header1.getRecordsCount());
        ILasHeader header2 = lasReader.getHeader();
        System.out.println("points: " + header2.getRecordsCount());

        long count = 0;
        while( libLas.hasNextPoint() && lasReader.hasNextPoint() ) {
            LasRecord dot1 = libLas.getNextPoint();
            System.out.println("LL: " + dot1.x + "/" + dot1.y + "/" + dot1.z);
            LasRecord dot2 = lasReader.getNextPoint();
            System.out.println("LR: " + dot2.x + "/" + dot2.y + "/" + dot2.z);
            System.out.println("LL.equals(LR) = " + LasUtils.lasRecordEqual(dot1, dot2));

            count++;
            if (count == 3) {
                break;
            }
        }
        System.out.println("***************");
        libLas.seek(0);
        lasReader.seek(0);
        count = 0;
        while( libLas.hasNextPoint() ) {
            LasRecord dot = libLas.getNextPoint();
            System.out.println("LL: " + dot.x + "/" + dot.y + "/" + dot.z);

            if (lasReader.hasNextPoint()) {
                LasRecord dot2 = lasReader.getNextPoint();
                System.out.println("LR: " + dot2.x + "/" + dot2.y + "/" + dot2.z);
            }
            libLas.seek(1);
            lasReader.seek(1);

            count++;
            if (count == 3) {
                break;
            }
        }

        // long position = 0;
        // LasRecord dot = libLas.getPointAt(position);
        // System.out.println(dot.x + "/" + dot.y + "/" + dot.z);
        // position += 1;
        // dot = libLas.getPointAt(position);
        // System.out.println(dot.x + "/" + dot.y + "/" + dot.z);
        // position += 1;
        // dot = libLas.getPointAt(position);
        // System.out.println(dot.x + "/" + dot.y + "/" + dot.z);
        System.out.println("***************");
        System.out.println(header1);
        System.out.println("***************");
        System.out.println(header2);

        libLas.close();
        lasReader.close();
    }

}
