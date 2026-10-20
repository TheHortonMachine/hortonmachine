/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.server.jobs;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * The command of the java processes started by the server, as the jobs and the kernels of the
 * notebooks: the same java, properties and classpath of the server.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class JavaCommand {

    private JavaCommand() {
    }

    /**
     * Write the java arguments file with the classpath of the server: in a file, since the classpath
     * can be longer than the command line allows on Windows.
     *
     * @param argsFile the file to write.
     */
    public static void writeArgsFile( File argsFile ) throws Exception {
        String classpath = System.getProperty("java.class.path").replace("\\", "\\\\");
        FileUtilities.writeFile("-cp\n\"" + classpath + "\"\n", argsFile);
    }

    /**
     * @param argsFile the arguments file written by {@link #writeArgsFile(File)}.
     * @param memory the maximum memory, as for the -Xmx option, ex. 2g.
     * @return the start of the command: java, the memory, the properties and the classpath; the main
     *         class and its arguments follow.
     */
    public static List<String> build( File argsFile, String memory ) {
        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-Xmx" + memory);
        command.add("-Xss64m");
        command.add("-Djava.awt.headless=true");
        // from java 18 the default charset, used to read the output, is UTF-8 and the console one may differ
        command.add("-Dstdout.encoding=" + Charset.defaultCharset().name());
        command.add("-Dstderr.encoding=" + Charset.defaultCharset().name());
        copyProperty(command, "java.util.logging.config.file");
        copyProperty(command, "java.library.path");
        command.add("@" + argsFile.getAbsolutePath());
        return command;
    }

    private static void copyProperty( List<String> command, String name ) {
        String value = System.getProperty(name);
        if (value != null && !value.isEmpty()) {
            command.add("-D" + name + "=" + value);
        }
    }
}
