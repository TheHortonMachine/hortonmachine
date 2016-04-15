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
package org.jgrasstools.grass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jgrasstools.grass.dtd64.Task;
import org.jgrasstools.grass.utils.GrassModuleRunnerWithScript;
import org.jgrasstools.grass.utils.GrassUtils;
import org.jgrasstools.grass.utils.Oms3CodeWrapper;

/**
 * Generator for grass wrapper code.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GrassCodeGenerator {

    private GrassCodeGenerator() {}

    @SuppressWarnings("nls")
    public static void main( String[] args ) throws Exception {

        File generationFolder = new File("/home/moovida/development/jgrasstools-hg/jgrasstools/grass/src/main/java/");
        String gisbase = "/usr/lib/grass64";
        String shell = "/bin/sh";

        if (!generationFolder.exists()) {
            throw new RuntimeException();
        }

        System.setProperty(GrassUtils.GRASS_ENVIRONMENT_GISBASE_KEY, gisbase);
        System.setProperty(GrassUtils.GRASS_ENVIRONMENT_SHELL_KEY, shell);

        File gisbaseFile = new File(gisbase);
        File binFolder = new File(gisbaseFile, "bin");
        File scriptsFolder = new File(gisbaseFile, "scripts");

        String mapsetForRun = GrassUtils.prepareMapsetForRun(false);

        List<File> allFiles = new ArrayList<>();
        File[] binFiles = binFolder.listFiles();
        List<File> binsList = Arrays.asList(binFiles);
        allFiles.addAll(binsList);
        boolean isWindows = GrassUtils.isWindows();
        if (!isWindows) {
            // in windows the scripts are linked from the bin folder through the win shell
            File[] scriptFiles = scriptsFolder.listFiles();
            List<File> scriptsList = Arrays.asList(scriptFiles);
            allFiles.addAll(scriptsList);
        }

        for( File binFile : allFiles ) {
            String binName = binFile.getName();
            if (GrassUtils.grassModulesToIgnore.contains(binName)) {
                continue;
            }
            if (binName.toLowerCase().endsWith("manifest")) {
                continue;
            } else if (binName.toLowerCase().endsWith("exe")) {
                binName = binName.replaceFirst("\\.exe", "");
            } else if (binName.toLowerCase().endsWith("bat")) {
                binName = binName.replaceFirst("\\.bat", "");
            } else if (isWindows && binName.toLowerCase().endsWith("sh")) {
                continue;
            }

            System.out.println("Generating class: " + binName);

            GrassModuleRunnerWithScript grassRunner = new GrassModuleRunnerWithScript(null, null);
            String result = grassRunner.runModule(new String[]{binFile.getAbsolutePath(), "--interface-description"},
                    mapsetForRun).trim();
            if (result.startsWith("WARNING")) {
                continue;
            }
            Task task = null;
            try {
                task = GrassUtils.getTask(result);
            } catch (Exception e) {
                // ignore
                System.err.println("Ignoring: " + binName);
                System.err.println("*********************************");
                System.err.println(e.getLocalizedMessage());
                System.err.println(result);
                System.err.println("*********************************");
                if (!result.startsWith("<?xml") || !result.endsWith("task>")) {
                    continue;
                }
            }
            Oms3CodeWrapper gen = new Oms3CodeWrapper(task);
            String oms3Class = gen.getGeneratedOms3Class();
            String classSafeName = gen.getClassSafeName();
            String moduleQualifiedStructure = GrassUtils.getModuleQualifiedStructure(classSafeName);
            File moduleFile = new File(generationFolder, moduleQualifiedStructure);
            File parentFile = moduleFile.getParentFile();
            parentFile.mkdirs();

            BufferedWriter classWriter = null;
            try {
                classWriter = new BufferedWriter(new FileWriter(moduleFile));
                classWriter.write(oms3Class);
            } finally {
                if(classWriter != null ) {
                    classWriter.close();
                }
            }
        }

    }

}
