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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;
import org.hortonmachine.gears.utils.processes.ELogStyle;
import org.hortonmachine.gears.utils.processes.IProcessListener;
import org.hortonmachine.gui.console.ConsoleMessageFilter;

import groovy.ui.GroovyMain;

/**
 * Executor of OMS scripts.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class StageScriptExecutor {

    public static final String ORG_HORTONMACHINE_MODULES = "org.hortonmachine.modules";

    private List<IProcessListener> listeners = new ArrayList<IProcessListener>();

    private String classPath;

    private boolean isRunning = false;

    private String javaExec;

    private static String nl = "\n";

    private StringBuilder logBuilder = new StringBuilder();

    public StageScriptExecutor( File jgtLibsFolder ) throws Exception {
        /*
         * get java exec
         */
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            File java = new File(javaHome, "bin/java");
            if (java.exists()) {
                javaExec = java.getAbsolutePath();
            } else {
                java = new File(javaHome, "bin/java.exe");
                if (java.exists()) {
                    javaExec = java.getAbsolutePath();
                } else {
                    javaExec = null;
                }
            }
        }

        if (javaExec == null) {
            javaExec = "java"; // hope ti is in the path
        }
        // File javaFile = new File("/home/hydrologis/SOFTWARE/JAVA/JDKS/CURRENT/bin/java");
        // javaExec = javaFile.getAbsolutePath();
        // if (!javaFile.getAbsolutePath().equals("java")) {
        // javaExec = javaFile.getAbsolutePath();
        // // javaExec = "\"" + javaFile.getAbsolutePath() + "\"";
        // } else {
        // javaExec = javaFile.getName();
        // }
        /*
         * get libraries
         */
        // File[] jarFiles = jgtLibsFolder.listFiles(new FilenameFilter(){
        // public boolean accept( File dir, String name ) {
        // return name.endsWith("jar");
        // }
        // });
        //
        // if (jarFiles == null) {
        // jarFiles = new File[0];
        // }
        //
        // Arrays.sort(jarFiles, new Comparator<File>(){
        // public int compare( File o1, File o2 ) {
        // if (o1.getName().startsWith("jgt-oms") && o2.getName().startsWith("jgt-")) {
        // return -1;
        // }
        // if (o2.getName().startsWith("jgt-oms") && o1.getName().startsWith("jgt-")) {
        // return 1;
        // }
        // if (o1.getName().startsWith("jgt-") && !o2.getName().startsWith("jgt-")) {
        // return 1;
        // }
        // if (o2.getName().startsWith("jgt-") && !o1.getName().startsWith("jgt-")) {
        // return -1;
        // }
        // return 0;
        // }
        // });
        //
        // StringBuilder cpBuilder = new StringBuilder();
        // for( int i = 0; i < jarFiles.length; i++ ) {
        // if (i == 0) {
        // cpBuilder.append(jarFiles[i].getAbsolutePath());
        // } else {
        // cpBuilder.append(File.pathSeparator).append(jarFiles[i].getAbsolutePath());
        // }
        // }
        // String classpathJars = "\"" + cpBuilder.toString() + File.pathSeparator + ".\"";

        File[] groovyJarFiles = jgtLibsFolder.listFiles(new FilenameFilter(){
            public boolean accept( File dir, String name ) {
                return name.toLowerCase().contains("groovy-");
            }
        });
        if (groovyJarFiles != null && groovyJarFiles.length > 0 ) {
            String groovyFiles = "";
            for( File file : groovyJarFiles ) {
                groovyFiles += file.getAbsolutePath() + File.pathSeparator;
            }

            String classpathJars = "\"" + groovyFiles + jgtLibsFolder.getAbsolutePath() + "/*" + File.pathSeparator + ".\"";
            classPath = classpathJars;
        } else {
            classPath = "\"." + File.pathSeparator + jgtLibsFolder.getAbsolutePath() + File.separatorChar + "*\"";
        }

    }

    /**
     * Execute an OMS script.
     * 
     * @param script
     *            the script file or the script string.
     * @param internalStream
     * @param errorStream
     * @param loggerLevelGui
     *            the log level as presented in the GUI, can be OFF|ON. This is
     *            not the OMS logger level, which in stead has to be picked from
     *            the {@link SpatialToolboxConstants#LOGLEVELS_MAP}.
     * @param ramLevel
     *            the heap size to use in megabytes.
     * @param encoding
     * @return the process.
     * @throws Exception
     */
    public Process exec( String sessionId, String script, final String loggerLevelGui, String ramLevel, String encoding )
            throws Exception {
        File scriptFile = new File(script);
        if (!scriptFile.exists()) {
            // if the file doesn't exist, it is a script, let's put it into a
            // file
            scriptFile = File.createTempFile("hm_script_", ".groovy");
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(scriptFile));
                bw.write(script);
            } finally {
                bw.close();
            }

        } else {
            // it is a script in a file, read it to log it
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();
            try {
                br = new BufferedReader(new FileReader(scriptFile));
                String line = null;
                while( (line = br.readLine()) != null ) {
                    sb.append(line).append(nl);
                }
            } finally {
                br.close();
            }
            script = sb.toString();
        }

        // tmp folder
        String tempdir = System.getProperty("java.io.tmpdir");
        File omsTmp = new File(tempdir + File.separator + "oms");
        if (!omsTmp.exists())
            omsTmp.mkdirs();

        List<String> arguments = new ArrayList<String>();
        arguments.add(javaExec);

        // ram usage
        String ramExpr = "-Xmx" + ramLevel + "m";
        arguments.add(ramExpr);

        if (encoding != null && encoding.length() > 0) {
            encoding = "-Dfile.encoding=" + encoding;
            arguments.add(encoding);
        }

        // modules jars
        // List<String> modulesJars = StageModulesManager.getInstance().getModulesJars(true);
        // StringBuilder sb = new StringBuilder();
        // for( String moduleJar : modulesJars ) {
        // sb.append(File.pathSeparator).append(moduleJar);
        // }
        // String modulesJarsString = sb.toString().replaceFirst(File.pathSeparator, "");
        // String resourcesFlag = "-Doms.sim.resources=\"" + modulesJarsString + "\"";
        // arguments.add(resourcesFlag);

        // all the arguments
        arguments.add("-cp");
        arguments.add(classPath);
        arguments.add(GroovyMain.class.getCanonicalName());
        // if (loggerLevelGui.equals(SpatialToolboxConstants.LOGLEVEL_GUI_ON)) {
        // arguments.add("-l");
        // arguments.add("FINEST");
        // }
        // arguments.add("-r");
        arguments.add(scriptFile.getAbsolutePath());

        // String tmpScriptFilesDir = System.getProperty("java.io.tmpdir");
        // File tmpScriptFolder = new File(tmpScriptFilesDir);
        // StringBuilder runSb = new StringBuilder();
        // for( String arg : arguments ) {
        // runSb.append(arg).append(" ");
        // }
        //
        // String[] args;
        // if (OsCheck.getOperatingSystemType() == OSType.Windows) {
        // File tmpRunFile = new File(tmpScriptFolder, "udig_spatialtoolbox_" + sessionId + ".bat");
        // FileUtilities.writeFile("@echo off\n" + runSb.toString(), tmpRunFile);
        // args = new String[]{"cmd", "/c", tmpRunFile.getAbsolutePath()};
        // } else {
        // File tmpRunFile = new File(tmpScriptFolder, "udig_spatialtoolbox_" + sessionId + ".sh");
        // FileUtilities.writeFile(runSb.toString(), tmpRunFile);
        // args = new String[]{"sh", tmpRunFile.getAbsolutePath()};
        // }

        // {javaFile, ramExpr, resourcesFlag, "-cp", classPath,
        // CLI.class.getCanonicalName(), "-r",
        // scriptFile.getAbsolutePath()};

        Logger.INSTANCE.insertDebug("", "Execution arguments:");
        for( String arg : arguments ) {
            Logger.INSTANCE.insertDebug("", arg);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        // work in home
        // processBuilder.directory(homeFile);

        // environment
        // Map<String, String> environment = processBuilder.environment();
        // environment.put("CLASSPATH", classPath);

        final Process process = processBuilder.start();
        logBuilder.setLength(0);

        StringBuilder preCommentsBuilder = new StringBuilder();
        String processName = "Process started: " + SpatialToolboxConstants.dateTimeFormatterYYYYMMDDHHMMSS.format(new Date());
        preCommentsBuilder.append(processName);
        preCommentsBuilder.append(nl);

        // command launched
        if (loggerLevelGui.equals(SpatialToolboxConstants.LOGLEVEL_GUI_ON)) {

            preCommentsBuilder.append("------------------------------>8----------------------------" + nl);
            preCommentsBuilder.append("Launching command: " + nl);
            preCommentsBuilder.append("------------------" + nl);
            List<String> command = processBuilder.command();
            for( String arg : command ) {
                preCommentsBuilder.append(arg);
                preCommentsBuilder.append(" ");
            }
            preCommentsBuilder.append("" + nl);
            preCommentsBuilder.append("(you can run the above from command line, customizing the content)" + nl);
            preCommentsBuilder.append("----------------------------------->8---------------------------------" + nl);
            preCommentsBuilder.append("" + nl);
            // script run
            preCommentsBuilder.append("Script run: " + nl);
            preCommentsBuilder.append("-----------" + nl);

            // remove datafolder reference
            preCommentsBuilder.append(script);
            preCommentsBuilder.append("" + nl);
            preCommentsBuilder.append("------------------------------>8----------------------------" + nl);
            preCommentsBuilder.append("" + nl);
            // // environment used
            // preCommentsBuilder.append("Environment used: " + nl);
            // preCommentsBuilder.append("-----------------" + nl);
            //
            // Set<Entry<String, String>> entrySet = environment.entrySet();
            // for( Entry<String, String> entry : entrySet ) {
            // preCommentsBuilder.append(entry.getKey());
            // preCommentsBuilder.append(" =\t");
            // preCommentsBuilder.append(entry.getValue()).append("" + nl);
            // }
            // preCommentsBuilder.append("------------------------------>8----------------------------"
            // + nl);
            // preCommentsBuilder.append("");
        }
        printMessage(preCommentsBuilder.toString(), ELogStyle.COMMENT);
        isRunning = true;

        new Thread("StageScriptExecutor->" + processName){
            public void run() {
                BufferedReader br = null;
                try {
                    InputStream is = process.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);
                    String line;
                    while( (line = br.readLine()) != null ) {
                        printMessage(line, ELogStyle.NORMAL);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    printException(loggerLevelGui, e);
                } finally {
                    if (br != null)
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    printMessage(
                            "Process finished: " + SpatialToolboxConstants.dateTimeFormatterYYYYMMDDHHMMSS.format(new Date()),
                            ELogStyle.COMMENT);
                    isRunning = false;
                    updateListenersForModuleStop();
                }
            }

        }.start();

        new Thread("StageScriptExecutor->" + processName + " -> Console printer"){
            public void run() {
                BufferedReader br = null;
                try {
                    String userCanceled = ModelsUserCancelException.class.getCanonicalName();
                    InputStream is = process.getErrorStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);
                    String line;
                    while( (line = br.readLine()) != null ) {
                        /*
                         * remove of ugly recurring geotools warnings. Not nice,
                         * but at least users do not get confused.
                         */
                        if (ConsoleMessageFilter.doRemove(line)) {
                            continue;
                        }
                        if (line.startsWith(userCanceled)) {
                            line = "Process cancelled by user.";
                        }
                        printMessage(line, ELogStyle.ERROR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    printException(loggerLevelGui, e);
                } finally {
                    if (br != null)
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            };
        }.start();

        return process;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void addProcessListener( IProcessListener listener ) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeProcessListener( IProcessListener listener ) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    private synchronized void printMessage( String message, ELogStyle style ) {
        String[] split = message.split(nl);
        for( String string : split ) {
            for( IProcessListener listener : listeners ) {
                listener.onMessage(string, style);
            }
        }
    }

    private void updateListenersForModuleStop() {
        for( IProcessListener listener : listeners ) {
            listener.onProcessStopped();
        }
    }

    private void printException( final String loggerLevelGui, Exception e ) {
        if (loggerLevelGui.equals(SpatialToolboxConstants.LOGLEVEL_GUI_ON)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            printMessage(sw.toString(), ELogStyle.ERROR);
        } else {
            printMessage(e.getLocalizedMessage(), ELogStyle.ERROR);
        }
    }

    public void killProcess( Process process ) {
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
