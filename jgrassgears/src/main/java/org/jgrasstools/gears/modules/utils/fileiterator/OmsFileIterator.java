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
package org.jgrasstools.gears.modules.utils.fileiterator;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_fileFilter_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_filesList_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_inFolder_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_outCurrentfile_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_pCode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_pRegex_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSFILEITERATOR_pathsList_DESCRIPTION;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
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

import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileTraversal;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@Description(OMSFILEITERATOR_DESCRIPTION)
@Documentation(OMSFILEITERATOR_DOCUMENTATION)
@Author(name = OMSFILEITERATOR_AUTHORNAMES, contact = OMSFILEITERATOR_AUTHORCONTACTS)
@Keywords(OMSFILEITERATOR_KEYWORDS)
@Label(OMSFILEITERATOR_LABEL)
@Name(OMSFILEITERATOR_NAME)
@Status(OMSFILEITERATOR_STATUS)
@License(OMSFILEITERATOR_LICENSE)
public class OmsFileIterator extends JGTModel {

    @Description(OMSFILEITERATOR_inFolder_DESCRIPTION)
    @UI(JGTConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description(OMSFILEITERATOR_pRegex_DESCRIPTION)
    @In
    public String pRegex = null;

    @Description(OMSFILEITERATOR_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSFILEITERATOR_fileFilter_DESCRIPTION)
    @In
    public FileFilter fileFilter = null;

    @Description(OMSFILEITERATOR_outCurrentfile_DESCRIPTION)
    @Out
    public String outCurrentfile = null;

    @Description(OMSFILEITERATOR_filesList_DESCRIPTION)
    @Out
    public List<File> filesList = null;

    @Description(OMSFILEITERATOR_pathsList_DESCRIPTION)
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
            CoordinateReferenceSystem crs = CRS.decode(pCode);
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
        OmsFileIterator fiter = new OmsFileIterator();
        fiter.inFolder = folder;
        fiter.pCode = epsg;
        fiter.process();
    }
}
