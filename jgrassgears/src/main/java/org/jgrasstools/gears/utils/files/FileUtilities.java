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
package org.jgrasstools.gears.utils.files;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
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

    /**
     * Read text from a file in one line.
     * 
     * @param filePath the path to the file to read.
     * @return the read string.
     * @throws IOException 
     */
    public static String readFile( String filePath ) throws IOException {
        return readFile(new File(filePath));
    }

    /**
     * Read text from a file in one line.
     * 
     * @param file the file to read.
     * @return the read string.
     * @throws IOException 
     */
    public static String readFile( File file ) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder(200);
            String line = null;
            while( (line = br.readLine()) != null ) {
                sb.append(line);
                sb.append("\n"); //$NON-NLS-1$
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }

    /**
     * Read text from a file to a list of lines.
     * 
     * @param file the path to the file to read.
     * @return the list of lines.
     * @throws IOException 
     */
    public static List<String> readFileToLinesList( String filePath ) throws IOException {
        return readFileToLinesList(new File(filePath));
    }

    /**
     * Read text from a file to a list of lines.
     * 
     * @param file the file to read.
     * @return the list of lines.
     * @throws IOException 
     */
    public static List<String> readFileToLinesList( File file ) throws IOException {
        BufferedReader br = null;
        List<String> lines = new ArrayList<String>();
        try {
            br = new BufferedReader(new FileReader(file));
            String line = null;
            while( (line = br.readLine()) != null ) {
                lines.add(line);
            }
            return lines;
        } finally {
            br.close();
        }
    }

    /**
     * Write text to a file in one line.
     * 
     * @param text the text to write.
     * @param file the file to write to.
     * @throws IOException 
     */
    public static void writeFile( String text, File file ) throws IOException {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(text);
        } finally {
            bw.close();
        }
    }

    /**
     * Write a list of lines to a file.
     * 
     * @param lines the list of lines to write.
     * @param file the path to the file to write to.
     * @throws IOException 
     */
    public static void writeFile( List<String> lines, String filePath ) throws IOException {
        writeFile(lines, new File(filePath));
    }
    
    /**
     * Write a list of lines to a file.
     * 
     * @param lines the list of lines to write.
     * @param file the file to write to.
     * @throws IOException 
     */
    public static void writeFile( List<String> lines, File file ) throws IOException {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            for( String line : lines ) {
                bw.write(line);
                bw.write("\n"); //$NON-NLS-1$
            }
        } finally {
            bw.close();
        }
    }

    public static String replaceBackSlashes( String path ) {
        return path.replaceAll("\\\\", "\\\\\\\\"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Returns the name of the file without the extention.
     * 
     * @param file the file to trim.
     * @return the name without extention.
     */
    public static String getNameWithoutExtention( File file ) {
        String name = file.getName();
        int lastDot = name.lastIndexOf("."); //$NON-NLS-1$
        name = name.substring(0, lastDot);
        return name;
    }
}
