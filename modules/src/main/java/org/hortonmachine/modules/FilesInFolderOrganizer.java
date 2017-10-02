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

import static org.hortonmachine.gears.libs.modules.HMConstants.LIST_READER;
import static org.hortonmachine.modules.FilesInFolderOrganizer.FilesInFolderOrganizer_AUTHORCONTACTS;
import static org.hortonmachine.modules.FilesInFolderOrganizer.FilesInFolderOrganizer_AUTHORNAMES;
import static org.hortonmachine.modules.FilesInFolderOrganizer.FilesInFolderOrganizer_DESCRIPTION;
import static org.hortonmachine.modules.FilesInFolderOrganizer.FilesInFolderOrganizer_KEYWORDS;
import static org.hortonmachine.modules.FilesInFolderOrganizer.FilesInFolderOrganizer_LABEL;
import static org.hortonmachine.modules.FilesInFolderOrganizer.FilesInFolderOrganizer_LICENSE;
import static org.hortonmachine.modules.FilesInFolderOrganizer.FilesInFolderOrganizer_NAME;
import static org.hortonmachine.modules.FilesInFolderOrganizer.FilesInFolderOrganizer_STATUS;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.files.FileTraversal;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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

@Description(FilesInFolderOrganizer_DESCRIPTION)
@Author(name = FilesInFolderOrganizer_AUTHORNAMES, contact = FilesInFolderOrganizer_AUTHORCONTACTS)
@Keywords(FilesInFolderOrganizer_KEYWORDS)
@Label(FilesInFolderOrganizer_LABEL)
@Name("_" + FilesInFolderOrganizer_NAME)
@Status(FilesInFolderOrganizer_STATUS)
@License(FilesInFolderOrganizer_LICENSE)
public class FilesInFolderOrganizer extends HMModel {

    @Description(FilesInFolderOrganizer_IN_FOLDER_DESCRIPTION)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inFolder;

    @Description(FilesInFolderOrganizer_OUT_FOLDER_DESCRIPTION)
    @UI(HMConstants.FOLDEROUT_UI_HINT)
    @In
    public String inOutputFolder;

    @Description(FilesInFolderOrganizer_doCountOnly_DESCRIPTION)
    @In
    public boolean doCountOnly = true;

    @Description(FilesInFolderOrganizer_FILE_FILTER_DESCRIPTION)
    @In
    public FileFilter fileFilter = null;

    public static final String FilesInFolderOrganizer_DESCRIPTION = "A module that iterates over files in a folder and organizes them into year/month/day folders.";
    public static final String FilesInFolderOrganizer_DOCUMENTATION = "";
    public static final String FilesInFolderOrganizer_KEYWORDS = "File";
    public static final String FilesInFolderOrganizer_LABEL = LIST_READER;
    public static final String FilesInFolderOrganizer_NAME = "filesinfolderorganizer";
    public static final int FilesInFolderOrganizer_STATUS = 10;
    public static final String FilesInFolderOrganizer_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String FilesInFolderOrganizer_AUTHORNAMES = "Andrea Antonello";
    public static final String FilesInFolderOrganizer_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String FilesInFolderOrganizer_IN_FOLDER_DESCRIPTION = "The folder on which to iterate";
    public static final String FilesInFolderOrganizer_OUT_FOLDER_DESCRIPTION = "The output folder";
    public static final String FilesInFolderOrganizer_FILE_FILTER_DESCRIPTION = "An optional file filter (used when developing).";
    public static final String FilesInFolderOrganizer_doCountOnly_DESCRIPTION = "Only count the files it would copy and exit";

    @Initialize
    public void initProcess() {
        doProcess = true;
    }

    @Execute
    public void process() throws Exception {

        checkFileExists(inOutputFolder);

        ArrayList<String> pathsList = new ArrayList<String>();

        new FileTraversal(fileFilter){
            public void onFile( final File f ) {
                pathsList.add(f.getAbsolutePath());
            }
        }.traverse(new File(inFolder));

        int filesNum = pathsList.size();
        pm.message("Files found to organize: " + filesNum);
        if (doCountOnly) {
            return;
        }

        DateTimeFormatter yearF = DateTimeFormat.forPattern("yyyy");
        DateTimeFormatter monthF = DateTimeFormat.forPattern("MM");
        DateTimeFormatter dayF = DateTimeFormat.forPattern("dd");

        TreeMap<String, TreeMap<String, TreeMap<String, List<String>>>> year2Month2Day2AbsolutpathMap = new TreeMap<>();
        pm.beginTask("Sorting by creation date...", filesNum);
        for( String path : pathsList ) {
            long creationTimestamp = FileUtilities.getCreationTimestamp(path);
            DateTime dateTime = new DateTime(creationTimestamp);

            String year = dateTime.toString(yearF);
            String month = dateTime.toString(monthF);
            String dayOfMonth = dateTime.toString(dayF);

            TreeMap<String, TreeMap<String, List<String>>> month2Day2AbsolutpathMap = year2Month2Day2AbsolutpathMap.get(year);
            if (month2Day2AbsolutpathMap == null) {
                month2Day2AbsolutpathMap = new TreeMap<>();
                year2Month2Day2AbsolutpathMap.put(year, month2Day2AbsolutpathMap);
            }
            TreeMap<String, List<String>> day2AbsolutpathMap = month2Day2AbsolutpathMap.get(month);
            if (day2AbsolutpathMap == null) {
                day2AbsolutpathMap = new TreeMap<>();
                month2Day2AbsolutpathMap.put(month, day2AbsolutpathMap);
            }
            List<String> filesList = day2AbsolutpathMap.get(dayOfMonth);
            if (filesList == null) {
                filesList = new ArrayList<>();
                day2AbsolutpathMap.put(dayOfMonth, filesList);
            }
            filesList.add(path);

            pm.worked(1);
        }
        pm.done();

        File outfolderFile = new File(inOutputFolder);
        // now sort and create folders

        pm.beginTask("Copy files to new locations...", filesNum);
        for( Entry<String, TreeMap<String, TreeMap<String, List<String>>>> yearEntry : year2Month2Day2AbsolutpathMap
                .entrySet() ) {
            String year = yearEntry.getKey();
            TreeMap<String, TreeMap<String, List<String>>> monthsMap = yearEntry.getValue();
            for( Entry<String, TreeMap<String, List<String>>> monthEntry : monthsMap.entrySet() ) {
                String month = monthEntry.getKey();
                TreeMap<String, List<String>> daysMap = monthEntry.getValue();
                for( Entry<String, List<String>> dayEntry : daysMap.entrySet() ) {
                    String day = dayEntry.getKey();
                    List<String> pathslist = dayEntry.getValue();
                    String newRelativePath = year + File.separator + month + File.separator + day + File.separator;
                    for( String p : pathslist ) {
                        File fromFile = new File(p);
                        File toFile = new File(outfolderFile, newRelativePath + fromFile.getName());

                        if (toFile.exists()) {
                            // check the sizes to make sure it is the same
                            long fromLength = fromFile.length();
                            long toLength = toFile.length();
                            if (fromLength == toLength) {
                                // do not copy, give warning and continue
                                pm.errorMessage("File has duplicate: " + fromFile.getName());
                                pm.worked(1);
                                continue;
                            } else {
                                throw new ModelsIOException(
                                        "Two files with same timestamp/name but different size were found: " + fromFile.getName(),
                                        this);
                            }
                        }

                        toFile.getParentFile().mkdirs();
                        FileUtilities.copyFile(fromFile, toFile);
                        // pm.message("copy " + file.getName() + " to " + newRelativePath +
                        // file.getName());
                        pm.worked(1);
                    }
                }
            }
        }
        pm.done();
    }

    public static void main( String[] args ) throws Exception {
        FilesInFolderOrganizer fo = new FilesInFolderOrganizer();
        fo.inFolder = "/home/hydrologis/Dropbox/Camera Uploads/";
        fo.inOutputFolder = "/home/hydrologis/Dropbox/cas/FOTO/";
        fo.doCountOnly = false;
        fo.process();
    }

}
