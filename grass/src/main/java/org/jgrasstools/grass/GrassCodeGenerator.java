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

import org.jgrasstools.grass.dtd64.Task;
import org.jgrasstools.grass.utils.GrassRunner;
import org.jgrasstools.grass.utils.GrassUtils;
import org.jgrasstools.grass.utils.Oms3CodeWrapper;

/**
 * Generator for grass wrapper code.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GrassCodeGenerator {

    public static void main( String[] args ) throws Exception {

        File generationFolder = new File("/home/moovida/development/jgrasstools-hg/grass/src/main/java/");

        System.setProperty(GrassUtils.GRASS_ENVIRONMENT_GISBASE_KEY, "/usr/lib/grass64");

        File gisbaseFile = new File("/usr/lib/grass64");
        File binFolder = new File(gisbaseFile, "bin");

        String[] mapsetForRun = GrassUtils.prepareMapsetForRun(false);
        String mapset = mapsetForRun[0];
        String gisrc = mapsetForRun[1];

        File[] binFiles = binFolder.listFiles();
        for( File binFile : binFiles ) {
            String binName = binFile.getName().replaceFirst("\\.exe", "");
            System.out.println("Generating class: " + binName);
            // if (GrassUtils.incompatibleGrassModules.contains(binName)) {
            // continue;
            // }

            // if (!binName.equals("nviz_cmd")) {
            // continue;
            // }

            GrassRunner grassRunner = new GrassRunner(null, null);
            String result = grassRunner.runModule(new String[]{binFile.getAbsolutePath(), "--interface-description"}, mapset,
                    gisrc);
            Task task;
            try {
                task = GrassUtils.getTask(result);
            } catch (Exception e) {
                // ignore
                System.out.println("Ignoring: " + binName);
                e.printStackTrace();
                continue;
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
                classWriter.close();
            }
        }

    }
}
