/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.io.grasslegacy.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;


/**
 * <p>
 * Various file utilities usefull when dealing with bytes, bits and numbers
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public class FileUtilities {

    public FileUtilities() {

    }

    public static byte[] double2bytearray( double rastervalue ) {
        long l = Double.doubleToLongBits(rastervalue);
        byte[] b = new byte[8];
        int shift = 64 - 8;
        for( int k = 0; k < 8; k++, shift -= 8 ) {
            b[k] = (byte) (l >>> shift);
        }
        return b;
    }

    public static BitSet fromByteArray( byte[] bytes ) {
        BitSet bits = new BitSet();
        for( int i = 0; i < bytes.length * 8; i++ ) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    public static BitSet fromByte( byte thebyte ) {
        BitSet bits = new BitSet();
        for( int i = 0; i < 8; i++ ) {
            if ((thebyte & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    public static byte[] BitSet2ByteArray( BitSet bs ) {
        byte[] bytes = new byte[bs.size() / 8 + 1];

        for( int i = 0; i < bs.size(); i++ )
            if (bs.get(i))
                bytes[i / 8] |= 1 << (i % 8);

        return bytes;
    }

    public static byte[] toByteArray( BitSet bits ) {
        byte[] bytes = new byte[bits.length() / 8 + 1];
        for( int i = 0; i < bits.length(); i++ ) {
            if (bits.get(i)) {
                bytes[bytes.length - i / 8 - 1] |= 1 << (i % 8);
            }
        }
        return bytes;
    }

    public static void copyFile( String fromFile, String toFile ) {
        File in = new File(fromFile);
        File out = new File(toFile);
        copyFile(in, out);
    }

    public static void copyFile( File in, File out ) {
        try {
            FileInputStream fis = new FileInputStream(in);
            FileOutputStream fos = new FileOutputStream(out);
            byte[] buf = new byte[1024];
            int i = 0;
            while( (i = fis.read(buf)) != -1 ) {
                fos.write(buf, 0, i);
            }
            fis.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static float arr2float( byte[] arr, int start ) {
        int i = 0;
        int len = 4;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for( i = start; i < (start + len); i++ ) {
            tmp[cnt] = arr[i];
            cnt++;
        }
        int accum = 0;
        i = 0;
        for( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return Float.intBitsToFloat(accum);
    }

    public static int arr2int( byte[] arr, int start ) {
        int low = arr[start] & 0xff;
        int high = arr[start + 1] & 0xff;
        return (int) (high << 8 | low);
    }

    public static long arr2long( byte[] arr, int start ) {
        int i = 0;
        int len = 4;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for( i = start; i < (start + len); i++ ) {
            tmp[cnt] = arr[i];
            cnt++;
        }
        long accum = 0;
        i = 0;
        for( int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return accum;
    }

    public static double arr2double( byte[] arr, int start ) {
        int i = 0;
        int len = 8;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for( i = start; i < (start + len); i++ ) {
            tmp[cnt] = arr[i];
            cnt++;
        }
        long accum = 0;
        i = 0;
        for( int shiftBy = 0; shiftBy < 64; shiftBy += 8 ) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return Double.longBitsToDouble(accum);
    }

    /**
     * Returns true if all deletions were successful. If a deletion fails, the method stops
     * attempting to delete and returns false.
     * 
     * @param filehandle
     * @return true if all deletions were successful
     */
    public static boolean deleteFileOrDir( File filehandle ) {

        if (filehandle.isDirectory()) {
            String[] children = filehandle.list();
            for( int i = 0; i < children.length; i++ ) {
                boolean success = deleteFileOrDir(new File(filehandle, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        boolean isdel = filehandle.delete();
        if (!isdel) {
            // if it didn't work, which often happens on windows systems,
            // remove on exit
            filehandle.deleteOnExit();
        }

        return isdel;
    }

    /**
     * Delete file or folder recursively on exit of the program
     * 
     * @param filehandle
     * @return true if all went well
     */
    public static boolean deleteFileOrDirOnExit( File filehandle ) {
        if (filehandle.isDirectory()) {
            String[] children = filehandle.list();
            for( int i = 0; i < children.length; i++ ) {
                boolean success = deleteFileOrDir(new File(filehandle, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        filehandle.deleteOnExit();
        return true;
    }

    /**
     * Read from an inoutstream and convert the readed stuff to a String. Usefull for text files
     * that are available as streams.
     * 
     * @param inputStream
     * @return the read string
     */
    public static String readInputStreamToString( InputStream inputStream ) {

        try {
            // Create the byte list to hold the data
            List<Byte> bytesList = new ArrayList<Byte>();

            byte b = 0;
            while( (b = (byte) inputStream.read()) != -1 ) {
                bytesList.add(b);
            }
            // Close the input stream and return bytes
            inputStream.close();

            byte[] bArray = new byte[bytesList.size()];
            for( int i = 0; i < bArray.length; i++ ) {
                bArray[i] = bytesList.get(i);
            }

            String file = new String(bArray);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String replaceBackSlashes( String path ) {
        return path.replaceAll("\\\\", "\\\\\\\\");
    }

    public static String getNameWithoutExtention( File file ) {
        String name = file.getName();
        int lastDot = name.lastIndexOf(".");
        name = name.substring(0, lastDot);
        return name;
    }
} // end of class
