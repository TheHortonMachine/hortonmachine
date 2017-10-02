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
package org.hortonmachine.gears.utils.files;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
* Find files in folders and subfolders, given a particular regex pattern.
*  
*/
public final class FilesFinder {

    private List<File> filesList = new ArrayList<File>();
    private final File file;
    private final String regex;

    /**
     * Constructor of {@link FilesFinder}.
     * 
     * @param file a file or folder to start from.
     * @param regex a regex to which to match the files names to.
     */
    public FilesFinder( File file, String regex ) {
        this.file = file;
        this.regex = regex;
    }

    public List<File> getFilesList() {
        return filesList;
    }

    public List<File> process() {
        if (file == null || !file.exists() || !file.canRead()) {
            throw new IllegalArgumentException("Directory not readable.");
        }
        if (!file.isDirectory()) {
            if (file.getName().matches(".*" + regex + ".*")) {
                filesList.add(file);
                return filesList;
            }
        }

        addToList(file);

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
