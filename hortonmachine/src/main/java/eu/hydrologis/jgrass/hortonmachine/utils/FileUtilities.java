/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.hortonmachine.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Various file utilities useful when dealing with bytes, bits and numbers
 * </p>
 * 
 * @author Andrea Antonello - www.hydrologis.com
 * @since 1.1.0
 */
public class FileUtilities {

    public static void copyFile( String fromFile, String toFile ) throws IOException {
        File in = new File(fromFile);
        File out = new File(toFile);
        copyFile(in, out);
    }

    public static void copyFile( File in, File out ) throws IOException {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int i = 0;
        while( (i = fis.read(buf)) != -1 ) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
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
     * @throws IOException 
     */
    public static String readInputStreamToString( InputStream inputStream ) throws IOException {
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
