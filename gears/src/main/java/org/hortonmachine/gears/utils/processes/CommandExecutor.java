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
package org.hortonmachine.gears.utils.processes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.StringUtilities;
import org.joda.time.DateTime;

/**
 * Executor of os commands.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CommandExecutor {
    private static String nl = "\n";

    private List<IProcessListener> listeners = new ArrayList<IProcessListener>();

    private String[] arguments;
    private boolean isRunning;
    private Process process;

    public CommandExecutor( String command ) {
        arguments = StringUtilities.parseCommand(command);
    }

    public CommandExecutor( String[] arguments ) {
        this.arguments = arguments;
    }

    public String[] getArguments() {
        return arguments;
    }

    public void exec() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
//        Map<String, String> environment = processBuilder.environment();
//        for( Entry<String, String> kv : environment.entrySet() ) {
//            System.out.println(kv.getKey() + ": " + kv.getValue());
//        }
//        
//        String path = System.getenv("PATH");
//        System.out.println("PATH: " + path);
        
        
        printMessage("Process started: " + DateTime.now().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS),
                ELogStyle.COMMENT);
        process = processBuilder.start();

        isRunning = true;

        new Thread(arguments[0]){
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
                    printException(e);
                } finally {
                    if (br != null)
                        try {
                            br.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    printMessage("Process finished: " + DateTime.now().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS),
                            ELogStyle.COMMENT);
                    isRunning = false;
                    updateListenersForModuleStop();
                }
            }

        }.start();

        new Thread(arguments[0] + " -> Console printer"){
            public void run() {
                BufferedReader br = null;
                try {
                    String userCanceled = ModelsUserCancelException.class.getCanonicalName();
                    InputStream is = process.getErrorStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);
                    String line;
                    while( (line = br.readLine()) != null ) {
//                        /*
//                         * remove of ugly recurring geotools warnings. Not nice,
//                         * but at least users do not get confused.
//                         */
//                        if (ConsoleMessageFilter.doRemove(line)) {
//                            continue;
//                        }
                        if (line.startsWith(userCanceled)) {
                            line = "Process cancelled by user.";
                        }
                        printMessage(line, ELogStyle.ERROR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    printException(e);
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
    }

    public void waitToFinish() throws InterruptedException {
        process.waitFor();
    }

    public Process getProcess() {
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

    private void printException( Exception e ) {
//        if (loggerLevelGui.equals(SpatialToolboxConstants.LOGLEVEL_GUI_ON)) {
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            e.printStackTrace(pw);
//            printMessage(sw.toString(), ELogStyle.ERROR);
//        } else {
        printMessage(e.getLocalizedMessage(), ELogStyle.ERROR);
//        }
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

    public static void main( String[] args ) throws Exception {
        String cmd = "echo $PATH";
//        String cmd = "docker run -v .:/workspace --user $(id -u):$(id -g) --rm -it osgeo/gdal /bin/bash -c \"cd /workspace; exec gdalinfo --formats\"";

        CommandExecutor exe = new CommandExecutor(cmd);
        exe.addProcessListener(new SystemoutProcessListener());
        exe.exec();
        exe.waitToFinish();
        System.out.println("DONE");
    }
}
