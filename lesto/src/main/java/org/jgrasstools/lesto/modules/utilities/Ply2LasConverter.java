/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.lesto.modules.utilities;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jgrasstools.gears.io.las.core.ALasWriter;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("Convert a ply file to las.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("convert, ply, lidar, las")
@Label(JGTConstants.LESTO + "/utilities")
@Name("ply2lasconverter")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class Ply2LasConverter extends JGTModel {
    @Description("Ply file path.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inPly = null;

    @Description("Output las file path.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outLas = null;

    private int pointCount = -1;

    private int binaryAddress = 0;

    private int columnsCount = 0;
    private int indexOfX = -1;
    private int indexOfY = -1;
    private int indexOfZ = -1;

    private final byte[] float64DataArray = new byte[8];
    private final ByteBuffer float64Bb = ByteBuffer.wrap(float64DataArray);
    private final byte[] float32DataArray = new byte[4];
    private final ByteBuffer float32Bb = ByteBuffer.wrap(float32DataArray);
    private final byte[] shortDataArray = new byte[2];
    private final ByteBuffer shortBb = ByteBuffer.wrap(shortDataArray);
    private final byte[] singleDataArray = new byte[1];
    private final ByteBuffer singleBb = ByteBuffer.wrap(singleDataArray);

    private int[] columnsSizesArrays;

    public static void main( String[] args ) throws Exception {
        Ply2LasConverter c = new Ply2LasConverter();
        c.inPly = "D:/data/UAV/mergedchunk.ply";
        c.outLas = "D:/data/UAV/mergedchunk.las";
        c.process();
    }

    @Execute
    public void process() throws Exception {
        checkNull(inPly);

        float64Bb.order(ByteOrder.LITTLE_ENDIAN);
        float32Bb.order(ByteOrder.LITTLE_ENDIAN);
        shortBb.order(ByteOrder.LITTLE_ENDIAN);
        singleBb.order(ByteOrder.LITTLE_ENDIAN);

        readHeader();

        double xMin = Double.POSITIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
        double zMin = Double.POSITIVE_INFINITY;
        double xMax = Double.NEGATIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double zMax = Double.NEGATIVE_INFINITY;

        File plyFile = new File(inPly);
        try (FileInputStream fis = new FileInputStream(plyFile); FileChannel fc = fis.getChannel();) {
            fc.position(binaryAddress);

            List<LasRecord> readPoints = new ArrayList<LasRecord>();

            pm.beginTask("Reading ply...", pointCount);
            for( int i = 0; i < pointCount; i++ ) {
                LasRecord dot = new LasRecord();
                readPoints.add(dot);

                for( int j = 0; j < columnsCount; j++ ) {
                    int columnSize = columnsSizesArrays[j];
                    float f = -9999f;
                    switch( columnSize ) {
                    case 8:
                        float64Bb.clear();
                        fc.read(float64Bb);
                        break;
                    case 4:
                        float32Bb.clear();
                        fc.read(float32Bb);
                        f = float32Bb.getFloat(0);
                        break;
                    case 2:
                        shortBb.clear();
                        fc.read(shortBb);
                        break;
                    case 1:
                        singleBb.clear();
                        fc.read(singleBb);
                        break;
                    default:
                        break;
                    }

                    if (j == indexOfX) {
                        dot.x = f;
                        xMin = min(xMin, f);
                        xMax = max(xMax, f);
                    } else if (j == indexOfY) {
                        dot.y = f;
                        yMin = min(yMin, f);
                        yMax = max(yMax, f);
                    } else if (j == indexOfZ) {
                        dot.z = f;
                        zMin = min(zMin, f);
                        zMax = max(zMax, f);
                    }
                }
                pm.worked(1);
            }
            pm.done();

            File outLasFile = new File(outLas);
            try (ALasWriter writer = new LasWriter(outLasFile, null)) {
                writer.setBounds(xMin, xMax, yMin, yMax, zMin, zMax);
                writer.open();

                pm.beginTask("Writing las...", pointCount);
                for( LasRecord dot : readPoints ) {
                    writer.addPoint(dot);
                    pm.worked(1);
                }
                pm.done();

            }
        }
    }

    private void readHeader() throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(inPly))) {
            List<Integer> columnsSizes = new ArrayList<Integer>();

            boolean vertexesDone = false;
            String line = null;
            while( (line = br.readLine()) != null ) {
                binaryAddress = binaryAddress + line.getBytes().length;
                line = line.trim();

                if (line.equals("end_header")) {
                    break;
                } else if (line.startsWith("format")) {
                    String[] split = line.split("\\s+");
                    if (!split[1].equals("binary_little_endian")) {
                        throw new ModelsIllegalargumentException("Only binary_little_endian is supported at the moment.", this);
                    }
                } else if (line.startsWith("element face")) {
                    vertexesDone = true;
                } else if (line.startsWith("element vertex")) {
                    String[] split = line.split("\\s+");
                    pointCount = Integer.parseInt(split[2]);
                } else if (line.startsWith("property")) {
                    if (vertexesDone) {
                        continue;
                    }
                    String[] split = line.split("\\s+");

                    /*
                    name        type        number of bytes
                    ---------------------------------------
                    int8       character                 1
                    uint8      unsigned character        1
                    int16      short integer             2
                    uint16     unsigned short integer    2
                    int32      integer                   4
                    uint32     unsigned integer          4
                    float32    single-precision float    4
                    float64    double-precision float    8
                    */
                    switch( split[1] ) {
                    case "int8":
                    case "uint8":
                    case "uchar":
                    case "char":
                        columnsSizes.add(1);
                        break;
                    case "int16":
                    case "uint16":
                        columnsSizes.add(2);
                        break;
                    case "int32":
                    case "uint32":
                    case "float32":
                    case "float":
                        columnsSizes.add(4);
                        break;
                    case "float64":
                        columnsSizes.add(8);
                        break;
                    default:
                        break;
                    }

                    if (split[2].equals("x")) {
                        indexOfX = columnsCount;
                    } else if (split[2].equals("y")) {
                        indexOfY = columnsCount;
                    } else if (split[2].equals("z")) {
                        indexOfZ = columnsCount;
                    }
                    columnsCount++;
                }

            }

            columnsSizesArrays = new int[columnsSizes.size()];
            for( int i = 0; i < columnsSizesArrays.length; i++ ) {
                columnsSizesArrays[i] = columnsSizes.get(i);
            }
        }
    }

}
