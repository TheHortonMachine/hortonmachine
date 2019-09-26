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
package org.hortonmachine.gears.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.joda.time.DateTime;

/**
 * Utilities class to zip and unzip folders.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.0
 */
public class CompressionUtilities {

    /**
     * Compress a folder and its contents.
     * 
     * @param srcFolder path to the folder to be compressed.
     * @param destZipFile path to the final output zip file.
     * @param addBaseFolder flag to decide whether to add also the provided base (srcFolder) folder or not.
     * @throws IOException 
     */
    static public void zipFolder( String srcFolder, String destZipFile, boolean addBaseFolder ) throws IOException {
        if (new File(srcFolder).isDirectory()) {
            try (FileOutputStream fileWriter = new FileOutputStream(destZipFile);
                    ZipOutputStream zip = new ZipOutputStream(fileWriter)) {
                addFolderToZip("", srcFolder, zip, addBaseFolder); //$NON-NLS-1$
            }
        } else {
            throw new IOException(srcFolder + " is not a folder.");
        }
    }

    /**
     * Compress a set of files and its contents if folders.
     * 
     * @param srcFiles
     *            the files to compress.
     * @param destZipFile
     *            path to the final output zip file.
     * @throws IOException
     */
    static public void zipFiles( File[] srcFiles, String destZipFile ) throws IOException {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;
        try {
            fileWriter = new FileOutputStream(destZipFile);
            zip = new ZipOutputStream(fileWriter);

            for( File file : srcFiles ) {
                if (file.isDirectory()) {
                    addFolderToZip("", file.getAbsolutePath(), zip, true); //$NON-NLS-1$
                } else {
                    addToZip("", file.getAbsolutePath(), zip);
                }
            }

        } finally {
            if (zip != null) {
                zip.flush();
                zip.close();
            }
            if (fileWriter != null)
                fileWriter.close();
        }
    }

    /**
     * Uncompress a compressed file to the contained structure.
     * 
     * @param zipFile the zip file that needs to be unzipped
     * @param destFolder the folder into which unzip the zip file and create the folder structure
     * @param addTimeStamp if <code>true</code>, the timestamp is added if the base folder already exists.
     * @return the name of the internal base folder or <code>null</code>.
     * @throws IOException 
     */
    public static String unzipFolder( String zipFile, String destFolder, boolean addTimeStamp ) throws IOException {
        String newFirstName = null;
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration< ? extends ZipEntry> zipEnum = zf.entries();

            String firstName = null;

            while( zipEnum.hasMoreElements() ) {
                ZipEntry item = (ZipEntry) zipEnum.nextElement();

                String itemName = item.getName();
                if (firstName == null) {
                    int firstSlash = itemName.indexOf('/');
                    if (firstSlash == -1) {
                        firstSlash = itemName.length();
                    }
                    firstName = itemName.substring(0, firstSlash);
                    newFirstName = firstName;
                    File baseFile = new File(destFolder + File.separator + firstName);
                    if (baseFile.exists()) {
                        if (addTimeStamp) {
                            newFirstName = firstName + "_"
                                    + new DateTime().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSScompact);
                        } else {
                            throw new IOException("Not overwriting existing: " + baseFile);
                        }
                    }
                }
                if (firstName == null) {
                    throw new IOException();
                }
                itemName = itemName.replaceFirst(firstName, newFirstName);

                if (item.isDirectory()) {
                    File newdir = new File(destFolder + File.separator + itemName);
                    if (!newdir.mkdir())
                        throw new IOException();
                } else {
                    String newfilePath = destFolder + File.separator + itemName;
                    File newFile = new File(newfilePath);
                    File parentFile = newFile.getParentFile();
                    if (!parentFile.exists()) {
                        if (!parentFile.mkdirs())
                            throw new IOException();
                    }
                    InputStream is = zf.getInputStream(item);
                    FileOutputStream fos = new FileOutputStream(newfilePath);
                    byte[] buffer = new byte[512];
                    int readchars = 0;
                    while( (readchars = is.read(buffer)) != -1 ) {
                        fos.write(buffer, 0, readchars);
                    }
                    is.close();
                    fos.close();
                }
            }
        }

        return newFirstName;
    }

    public static File unzipSingleFile( String zipFile, String destFolder, boolean addTimeStamp ) throws IOException {
        File newFile = null;
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration< ? extends ZipEntry> zipEnum = zf.entries();

            while( zipEnum.hasMoreElements() ) {
                ZipEntry item = (ZipEntry) zipEnum.nextElement();

                String itemName = item.getName();
                String newfilePath = destFolder + File.separator + itemName;
                newFile = new File(newfilePath);
                File parentFile = newFile.getParentFile();
                if (!parentFile.exists()) {
                    if (!parentFile.mkdirs())
                        throw new IOException();
                }
                InputStream is = zf.getInputStream(item);
                FileOutputStream fos = new FileOutputStream(newfilePath);
                byte[] buffer = new byte[512];
                int readchars = 0;
                while( (readchars = is.read(buffer)) != -1 ) {
                    fos.write(buffer, 0, readchars);
                }
                is.close();
                fos.close();
            }
        }

        return newFile;
    }

    static private void addToZip( String path, String srcFile, ZipOutputStream zip ) throws IOException {
        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip, true);
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = null;
            try {
                in = new FileInputStream(srcFile);
                String relPath = path + File.separator;
                if (path.length() == 0)
                    relPath = "";
                zip.putNextEntry(new ZipEntry(relPath + folder.getName()));
                while( (len = in.read(buf)) > 0 ) {
                    zip.write(buf, 0, len);
                }
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }

    static private void addFolderToZip( String path, String srcFolder, ZipOutputStream zip, boolean addFolder )
            throws IOException {
        File folder = new File(srcFolder);
        String listOfFiles[] = folder.list();
        for( int i = 0; i < listOfFiles.length; i++ ) {
            String folderPath = null;
            if (path.length() < 1) {
                if (addFolder)
                    folderPath = folder.getName();
                else
                    folderPath = "";
            } else {
                folderPath = path + File.separator + folder.getName();
            }
            String srcFile = srcFolder + File.separator + listOfFiles[i];
            addToZip(folderPath, srcFile, zip);
        }
    }

}