/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_AUTHORNAMES;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_FILES_LIST_DESCRIPTION;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_FILE_FILTER_DESCRIPTION;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_IN_FOLDER_DESCRIPTION;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_KEYWORDS;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_LABEL;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_LICENSE;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_NAME;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_OUT_CURRENT_FILE_DESCRIPTION;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_PATHS_LIST_DESCRIPTION;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_P_CODE_DESCRIPTION;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_P_REGEX_DESCRIPTION;
import static org.hortonmachine.gears.modules.utils.fileiterator.OmsFileIterator.OMSFILEITERATOR_STATUS;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.files.FileTraversal;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSFILEITERATOR_DESCRIPTION)
@Author(name = OMSFILEITERATOR_AUTHORNAMES, contact = OMSFILEITERATOR_AUTHORCONTACTS)
@Keywords(OMSFILEITERATOR_KEYWORDS)
@Label(OMSFILEITERATOR_LABEL)
@Name("_" + OMSFILEITERATOR_NAME)
@Status(OMSFILEITERATOR_STATUS)
@License(OMSFILEITERATOR_LICENSE)
public class FileIterator extends HMModel {

    @Description(OMSFILEITERATOR_IN_FOLDER_DESCRIPTION)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description(OMSFILEITERATOR_P_REGEX_DESCRIPTION)
    @In
    public String pRegex = null;

    @Description(OMSFILEITERATOR_P_CODE_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSFILEITERATOR_FILE_FILTER_DESCRIPTION)
    @In
    public FileFilter fileFilter = null;

    @Description(OMSFILEITERATOR_OUT_CURRENT_FILE_DESCRIPTION)
    @Out
    public String outCurrentfile = null;

    @Description(OMSFILEITERATOR_FILES_LIST_DESCRIPTION)
    @Out
    public List<File> filesList = null;

    @Description(OMSFILEITERATOR_PATHS_LIST_DESCRIPTION)
    @Out
    public List<String> pathsList = null;

    private int fileIndex = 0;

    private String prjWkt;

    @Initialize
    public void initProcess() {
        doProcess = true;
    }

    @Execute
    public void process() throws Exception {
        if (pCode != null) {
            CoordinateReferenceSystem crs = CrsUtilities.getCrsFromEpsg(pCode);
            prjWkt = crs.toWKT();
        }

        if (filesList == null) {
            filesList = new ArrayList<File>();
            pathsList = new ArrayList<String>();

            new FileTraversal(fileFilter){
                public void onFile( final File f ) {
                    if (pRegex == null) {
                        filesList.add(f);
                        pathsList.add(f.getAbsolutePath());
                    } else {
                        if (f.getName().matches(".*" + pRegex + ".*")) { //$NON-NLS-1$//$NON-NLS-2$
                            filesList.add(f);
                            pathsList.add(f.getAbsolutePath());
                        }
                    }
                }
            }.traverse(new File(inFolder));

            if (prjWkt != null) {
                for( File file : filesList ) {
                    String nameWithoutExtention = FileUtilities.getNameWithoutExtention(file);
                    if (nameWithoutExtention != null) {
                        File prjFile = new File(file.getParentFile(), nameWithoutExtention + ".prj"); //$NON-NLS-1$
                        if (!prjFile.exists()) {
                            // create it
                            FileUtilities.writeFile(prjWkt, prjFile);
                        }
                    }
                }
            }

        }

        if (filesList.size() > fileIndex)
            outCurrentfile = filesList.get(fileIndex).getAbsolutePath();

        if (fileIndex == filesList.size() - 1) {
            doProcess = false;
        }
        fileIndex++;

    }

    /**
     * Utility to add to all found files in a given folder the prj file following the supplied epsg.
     * 
     * @param folder the folder to browse.
     * @param epsg the epsg from which to take the prj.
     * @throws Exception
     */
    public static void addPrj( String folder, String epsg ) throws Exception {
        FileIterator fiter = new FileIterator();
        fiter.inFolder = folder;
        fiter.pCode = epsg;
        fiter.process();
    }
    
    public static void main( String[] args ) throws Exception {
        addPrj("", "");
    }
}
