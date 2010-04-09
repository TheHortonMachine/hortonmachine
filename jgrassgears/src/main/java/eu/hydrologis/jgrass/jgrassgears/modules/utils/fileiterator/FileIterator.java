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
package eu.hydrologis.jgrass.jgrassgears.modules.utils.fileiterator;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;
import eu.hydrologis.jgrass.jgrassgears.libs.modules.HMModel;

@Description("A module that iterates over files in a folder")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Iterator, File")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class FileIterator extends HMModel {

    @Description("The folder on which to iterate")
    @In
    public String inFolder;

    @Description("Regular expression to match the file names.")
    @In
    public String pRegex = null;

    @Description("The current file of the list of files in the folder.")
    @Out
    public String outCurrentfile = null;

    private List<File> filesList = null;
    private int fileIndex = 0;

    @Execute
    public void process() {
        if (filesList == null) {
            File folderFile = new File(inFolder);
            File[] listFiles = folderFile.listFiles(new FilenameFilter(){
                public boolean accept( File dir, String name ) {
                    if (pRegex == null) {
                        // all files
                        return true;
                    } else {
                        if (name.matches(pRegex)) {
                            return true;
                        }
                        return false;
                    }
                }
            });
            filesList = Arrays.asList(listFiles);
        }

        outCurrentfile = filesList.get(fileIndex).getAbsolutePath();

        if (fileIndex == filesList.size() - 1) {
            doProcess = false;
        }
        fileIndex++;

    }

}
