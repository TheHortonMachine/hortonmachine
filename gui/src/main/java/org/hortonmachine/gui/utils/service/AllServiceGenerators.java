package org.hortonmachine.gui.utils.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.hortonmachine.gears.JGrassGears;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.HortonMachine;
import org.hortonmachine.lesto.Lesto;
import org.hortonmachine.modules.Modules;

/**
 * Regenerates the single consolidated META-INF/services HMModel file in the gui module.
 *
 * <p>Run with an optional first argument pointing to the project root. If omitted,
 * the class walks up from the working directory until it finds the root (identified
 * by containing gears/, modules/, hmachine/ and lesto/ subdirectories).
 */
public class AllServiceGenerators {

    private static final String SERVICE_FILE =
            "gui/src/main/resources/META-INF/services/org.hortonmachine.gears.libs.modules.HMModel";

    public static void main( String[] args ) throws IOException {
        File projectRoot;
        if (args.length > 0) {
            projectRoot = new File(args[0]).getCanonicalFile();
        } else {
            projectRoot = findProjectRoot(new File(".").getCanonicalFile());
        }
        if (projectRoot == null) {
            throw new IOException(
                    "Could not locate project root. Pass it as the first argument.");
        }
        System.out.println("Project root: " + projectRoot);

        File serviceFile = new File(projectRoot, SERVICE_FILE);
        if (!serviceFile.exists()) {
            throw new IOException("Service file not found: " + serviceFile);
        }

        List<String> names = new ArrayList<>();
        collectHMModels(names, JGrassGears.getInstance().moduleName2Class.entrySet());
        collectHMModels(names, Modules.getInstance().moduleName2Class.entrySet());
        collectHMModels(names, HortonMachine.getInstance().moduleName2Class.entrySet());
        collectHMModels(names, Lesto.getInstance().moduleName2Class.entrySet());

        Collections.sort(names);
        FileUtilities.writeFile(names, serviceFile);
        System.out.println("Updated " + serviceFile + " (" + names.size() + " entries).");
    }

    private static void collectHMModels( List<String> names,
            Set<Entry<String, Class< ? >>> entries ) {
        for( Entry<String, Class< ? >> entry : entries ) {
            if (HMModel.class.isAssignableFrom(entry.getValue())) {
                names.add(entry.getValue().getCanonicalName());
            }
        }
    }

    private static File findProjectRoot( File dir ) {
        while (dir != null) {
            if (new File(dir, "gears").isDirectory()
                    && new File(dir, "modules").isDirectory()
                    && new File(dir, "hmachine").isDirectory()
                    && new File(dir, "lesto").isDirectory()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        return null;
    }
}
