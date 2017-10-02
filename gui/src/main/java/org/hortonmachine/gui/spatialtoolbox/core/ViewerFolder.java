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
package org.hortonmachine.gui.spatialtoolbox.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * A folder for the treeviewer of the modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ViewerFolder {
    private final String name;

    private List<ViewerFolder> subFolders = new ArrayList<ViewerFolder>();
    private List<ViewerModule> modules = new ArrayList<ViewerModule>();

    private ViewerFolder parentFolder;

    public ViewerFolder( String name ) {
        this.name = name;
    }

    public void setParentFolder( ViewerFolder parentFolder ) {
        this.parentFolder = parentFolder;
    }

    public ViewerFolder getParentFolder() {
        return parentFolder;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name.replaceFirst("/", "");
    }

    public void addSubFolder( ViewerFolder subFolder ) {
        if (!subFolders.contains(subFolder)) {
            subFolder.setParentFolder(this);
            subFolders.add(subFolder);
        }
    }

    public void addModule( ViewerModule module ) {
        if (!modules.contains(module)) {
            module.setParentFolder(this);
            modules.add(module);
        }
    }

    public List<ViewerFolder> getSubFolders() {
        return subFolders;
    }

    public List<ViewerModule> getModules() {
        return modules;
    }

    public static List<ViewerFolder> hashmap2ViewerFolders( TreeMap<String, List<ModuleDescription>> availableModules,
            String filterText, boolean loadExperimental ) {
        List<ViewerFolder> folders = new ArrayList<ViewerFolder>();

        HashMap<String, ViewerFolder> tmpFoldersMap = new HashMap<String, ViewerFolder>();

        Set<Entry<String, List<ModuleDescription>>> entrySet = availableModules.entrySet();
        for( Entry<String, List<ModuleDescription>> entry : entrySet ) {
            String key = entry.getKey();
            List<ModuleDescription> md = entry.getValue();

            String separator = "/";
            String[] keySplit = key.split(separator);

            int lastSlash = key.lastIndexOf(separator);
            String base;
            if (lastSlash == -1) {
                base = key;
            } else {
                base = key.substring(0, lastSlash);
            }
            String mainKey = base + separator;

            ViewerFolder mainFolder = tmpFoldersMap.get(mainKey);
            if (mainFolder == null) {
                mainFolder = new ViewerFolder(mainKey);
                folders.add(mainFolder);
                tmpFoldersMap.put(mainKey, mainFolder);
            }

            int from = keySplit.length - 1;
            if (from == 0) {
                from = 1;
            }
            for( int i = from; i < keySplit.length; i++ ) {
                ViewerFolder tmpFolder = tmpFoldersMap.get(keySplit[i]);
                if (tmpFolder == null) {
                    tmpFolder = new ViewerFolder(keySplit[i]);
                    StringBuilder keyB = new StringBuilder();
                    for( int j = 0; j <= i; j++ ) {
                        keyB.append(keySplit[j]).append(separator);
                    }
                    tmpFoldersMap.put(keyB.toString(), tmpFolder);
                }
                mainFolder.addSubFolder(tmpFolder);
                mainFolder = tmpFolder;
            }

            // add the module to the last available if the filter allows it
            for( ModuleDescription moduleDescription : md ) {
                String moduleNameLC = moduleDescription.getName().toLowerCase();
                if (filterText != null && filterText.length() > 0 && !moduleNameLC.contains(filterText.toLowerCase())) {
                    continue;
                }
                if (moduleDescription.getStatus() == ModuleDescription.Status.experimental && !loadExperimental) {
                    continue;
                }
                mainFolder.addModule(new ViewerModule(moduleDescription));
            }
        }

        // remove empty folders
        Iterator<ViewerFolder> folderIterator = folders.iterator();
        while( folderIterator.hasNext() ) {
            ViewerFolder folder = folderIterator.next();
            List<ViewerFolder> subFoldersList = folder.getSubFolders();
            // first check the subfolders
            Iterator<ViewerFolder> subFoldersIterator = subFoldersList.iterator();
            while( subFoldersIterator.hasNext() ) {
                ViewerFolder subFolder = subFoldersIterator.next();
                List<ViewerFolder> subSubFoldersList = subFolder.getSubFolders();
                if (subFolder.getModules().isEmpty() && subSubFoldersList.isEmpty()) {
                    subFoldersIterator.remove();
                    continue;
                }
            }
            // then check the main folders
            if (folder.getModules().isEmpty() && subFoldersList.isEmpty()) {
                folderIterator.remove();
                continue;
            }

        }

        return folders;
    }

}
