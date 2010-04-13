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
package eu.hydrologis.jgrass.jgrassgears.utils.files;
import java.util.*;
import java.io.*;

/**
* Find files in folders and subfolders, given a particular regex pattern.
*  
*/
public final class FilesFinder {

    private List<File> filesList = new ArrayList<File>();
    private final File folder;
    private final String regex;

    public static void main( String... aArgs ) throws FileNotFoundException {
        File startingDirectory = new File("/home/moovida/data/adf");
        List<File> files = new FilesFinder(startingDirectory, ".adf").process();

        // print out all file names, in the the order of File.compareTo()
        for( File file : files ) {
            System.out.println(file);
        }
    }

    public FilesFinder( File folder, String regex ) {
        this.folder = folder;
        this.regex = regex;
    }

    public List<File> getFilesList() {
        return filesList;
    }

    public List<File> process() {
        if (folder == null || !folder.exists() || !folder.isDirectory() || !folder.canRead()) {
            throw new IllegalArgumentException("Directory not readable.");
        }

        addToList(folder);

        return filesList;
    }

    private void addToList( File folder ) {
        File[] filesArray = folder.listFiles();
        for( File file : filesArray ) {
            if (file.getName().matches(".*" + regex + ".*") && !file.isDirectory()) {
                filesList.add(file);
            }
            if (file.isDirectory()) {
                addToList(file);
            }
        }
    }

}
