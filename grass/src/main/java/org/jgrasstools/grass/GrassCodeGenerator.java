package org.jgrasstools.grass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.jgrasstools.grass.dtd64.Task;
import org.jgrasstools.grass.utils.GrassRunner;
import org.jgrasstools.grass.utils.GrassUtils;
import org.jgrasstools.grass.utils.Oms3CodeWrapper;

public class GrassCodeGenerator {

    public static void main( String[] args ) throws Exception {

        File generationFolder = new File("/home/moovida/development/jgrasstools-hg/grass/src/main/java/");

        System.setProperty(GrassUtils.GRASS_ENVIRONMENT_GISBASE_KEY, "/usr/lib/grass64");

        File gisbaseFile = new File("/usr/lib/grass64");
        File binFolder = new File(gisbaseFile, "bin");

        File[] binFiles = binFolder.listFiles();
        for( File binFile : binFiles ) {
            String binName = binFile.getName().replaceFirst("\\.exe", "");
            if (GrassUtils.incompatibleGrassModules.contains(binName)) {
                continue;
            }

            GrassRunner grassRunner = new GrassRunner(null, null, false);
            String result = grassRunner.runModule(new String[]{binFile.getAbsolutePath(), "--interface-description"});
            Task task = GrassUtils.getTask(result);

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
