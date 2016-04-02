package org.jgrasstools.grass.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

/**
 * Utility methods ported from the Sextante library.
 * 
 * <p>Modified to work with JGrasstools.</p>
 * 
 * @author Victor Olaya
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GrassUtilsSextante {

    private GrassUtilsSextante() {}

    /**
     * Creates a compact startup script for GRASS. 
     * 
     * <p>The script code created may vary, depending on the operating system. This also
     * creates a temporary GRASS settings file (GISCR).
     * 
     * @param gisBase the gisbase path.
     * @param mapsetFolder the mapset inside which the process is run.
     * @param shell the shell to use (ex. /bin/sh for linux, C:\GRASS-64\msys\bin\sh.exe for windows).
     * @return The file handler for the created script, NULL on failure.
     * @throws IOException 
     */
    public static File createGrassModuleRunScript( String grassCommand, String gisBase, String mapsetFolder, String shell )
            throws IOException {
        boolean isWindows = GrassUtils.isWindows();
        boolean isUnix = GrassUtils.isUnix();
        boolean isMac = GrassUtils.isMacOSX();

        BufferedWriter output = null;

        File script = null;
        File gisrc = null;

        UUID id;
        String tmpPrefix;
        String tmpSuffix;
        String tmpExtension;
        String tmpBase;
        String tmpName;

        // Create temporary script file in system's temp dir
        id = UUID.randomUUID();
        tmpPrefix = new String(GrassUtils.TMP_PREFIX);
        tmpSuffix = new String("_" + id);
        tmpBase = new String(System.getProperty("java.io.tmpdir"));
        if (isWindows) {
            tmpExtension = new String("bat");
        } else {
            tmpExtension = new String("sh");
        }
        if (tmpBase.endsWith(File.separator)) {
            tmpName = new String(tmpBase + File.separator + tmpPrefix + tmpSuffix.replace('-', '_') + "." + tmpExtension);
        } else {
            tmpName = new String(tmpBase + File.separator + tmpPrefix + tmpSuffix.replace('-', '_') + "." + tmpExtension);
        }
        script = new File(tmpName);
        script.deleteOnExit();

        // Create a temporary GISRC file in system's temp dir
        id = UUID.randomUUID();
        tmpPrefix = new String(GrassUtils.TMP_PREFIX);
        tmpSuffix = new String("_" + id);
        tmpBase = new String(System.getProperty("java.io.tmpdir"));
        if (tmpBase.endsWith(File.separator)) {
            tmpName = new String(tmpBase + File.separator + tmpPrefix + tmpSuffix.replace('-', '_') + ".gisrc");
        } else {
            tmpName = new String(tmpBase + File.separator + tmpPrefix + tmpSuffix.replace('-', '_') + ".gisrc");
        }
        gisrc = new File(tmpName);
        gisrc.deleteOnExit();

        // Get GISDBASE, LOCATION and MAPSET from String parameter
        File tmpMapsetFile = new File(mapsetFolder);
        File tmpLocationFile = tmpMapsetFile.getParentFile();
        File tmpGrassdbFile = tmpLocationFile.getParentFile();

        final String mapset = tmpMapsetFile.getName();
        final String location = tmpLocationFile.getName();
        final String gisdbase = tmpGrassdbFile.getAbsolutePath();

        // Write the temporary GISRC file to use in this session
        try {
            output = new BufferedWriter(new FileWriter(gisrc));
            output.write("GISDBASE: " + gisdbase + "\n");
            output.write("LOCATION_NAME: " + location + "\n");
            output.write("MAPSET: " + mapset + "\n");
            output.write("GRASS_GUI: text\n");
        } catch (final Exception e) {
            return null;
        } finally {
            if(output != null) {
                output.close();
            }
        }

        // Write the startup script
        if (isUnix || isMac) {
            // Startup script for *nix like systems with built-in shell interpreter
            try {
                if (shell == null) {
                    shell = "/bin/sh";
                }

                output = new BufferedWriter(new FileWriter(script));
                output.write("#!" + shell + "\n");
                output.write("export GISRC=\"" + gisrc.getAbsolutePath() + "\"\n");
                output.write("export GISBASE=\"" + gisBase + "\"\n");
                output.write("export GRASS_PROJSHARE=\"" + gisBase + File.separator + "share" + File.separator + "proj" + "\"\n");
                output.write("export GRASS_MESSAGE_FORMAT=text\n");
                output.write("export GRASS_SH=" + shell + "\n");
                output.write("export GRASS_PERL=/usr/bin/perl\n");
                output.write("export GIS_LOCK=$$\n");
                output.write("\n");
                output.write("if [ \"$LC_ALL\" ] ; then\n");
                output.write("\tLCL=`echo \"$LC_ALL\" | sed 's/\\(..\\)\\(.*\\)/\\1/'`\n");
                output.write("elif [ \"$LC_MESSAGES\" ] ; then\n");
                output.write("\tLCL=`echo \"$LC_MESSAGES\" | sed 's/\\(..\\)\\(.*\\)/\\1/'`\n");
                output.write("else\n");
                output.write("\tLCL=`echo \"$LANG\" | sed 's/\\(..\\)\\(.*\\)/\\1/'`\n");
                output.write("fi\n");
                output.write("\n");
                output.write("if [ -n \"$GRASS_ADDON_PATH\" ] ; then\n");
                output.write("\tPATH=\"" + gisBase + "/bin:" + gisBase + "/scripts:$GRASS_ADDON_PATH:$PATH\"\n");
                output.write("else\n");
                output.write("\tPATH=\"" + gisBase + "/bin:" + gisBase + "/scripts:$PATH\"\n");
                output.write("fi\n");
                output.write("export PATH\n");
                output.write("\n");
                if (isMac) {
                    output.write("if [ ! \"$DYLD_LIBRARY_PATH\" ] ; then\n");
                    output.write("\tDYLD_LIBRARY_PATH=\"$GISBASE/lib\"\n");
                    output.write("else\n");
                    output.write("\tDYLD_LIBRARY_PATH=\"$GISBASE/lib:$DYLD_LIBRARY_PATH\"\n");
                    output.write("fi\n");
                    output.write("export DYLD_LIBRARY_PATH\n");
                } else {
                    output.write("if [ ! \"$LD_LIBRARY_PATH\" ] ; then\n");
                    output.write("\tLD_LIBRARY_PATH=\"$GISBASE/lib\"\n");
                    output.write("else\n");
                    output.write("\tLD_LIBRARY_PATH=\"$GISBASE/lib:$LD_LIBRARY_PATH\"\n");
                    output.write("fi\n");
                    output.write("export LD_LIBRARY_PATH\n");
                }
                output.write("\n");
                output.write("if [ ! \"$GRASS_PYTHON\" ] ; then\n");
                output.write("\tGRASS_PYTHON=python\n");
                output.write("fi\n");
                output.write("export GRASS_PYTHON\n");
                output.write("if [ ! \"$PYTHONPATH\" ] ; then\n");
                output.write("\tPYTHONPATH=\"$GISBASE/etc/python\"\n");
                output.write("else\n");
                output.write("\tPYTHONPATH=\"$GISBASE/etc/python:$PYTHONPATH\"\n");
                output.write("fi\n");
                output.write("export PYTHONPATH\n");
                output.write("\n");
                output.write("if [ ! \"$GRASS_GNUPLOT\" ] ; then\n");
                output.write("\tGRASS_GNUPLOT=\"gnuplot -persist\"\n");
                output.write("\texport GRASS_GNUPLOT\n");
                output.write("fi\n");
                output.write("\n");
                output.write("if [ \"$GRASS_FONT_CAP\" ] && [ ! -f \"$GRASS_FONT_CAP\" ] ; then\n");
                output.write("\tg.mkfontcap\n");
                output.write("fi\n");
                output.write("\n");
                output.write("g.gisenv set=\"MAPSET=" + mapset + "\"\n");
                output.write("g.gisenv set=\"LOCATION=" + location + "\"\n");
                output.write("g.gisenv set=\"LOCATION_NAME=" + location + "\"\n");
                output.write("g.gisenv set=\"GISDBASE=" + gisdbase + "\"\n");
                output.write("g.gisenv set=\"GRASS_GUI=text\"\n");
                output.write("\n");
                output.write("\n");
                output.write(grassCommand + "\n");
                output.write("\n");
            } catch (final Exception e) {
                return null;
            } finally {
                output.close();
            }
        } else {// Windows

            // Need a temporary text file for process communication
            id = UUID.randomUUID();
            tmpPrefix = new String(GrassUtils.TMP_PREFIX);
            tmpSuffix = new String("_" + id);
            tmpBase = new String(System.getProperty("java.io.tmpdir"));
            if (tmpBase.endsWith(File.separator)) {
                tmpName = new String(tmpBase + File.separator + tmpPrefix + tmpSuffix.replace('-', '_') + ".msg");
            } else {
                tmpName = new String(tmpBase + File.separator + tmpPrefix + tmpSuffix.replace('-', '_') + ".msg");
            }
            File m_ComFile = new File(tmpName);
            m_ComFile.deleteOnExit();

            // Write windows startup script
            String shToolsPath = null;
            if (shell != null && shell.contains(File.separator)) {
                shToolsPath = shell;
                shToolsPath = shToolsPath.substring(0, shToolsPath.lastIndexOf(File.separator));
            }
            try {
                m_ComFile.createNewFile();
                output = new BufferedWriter(new FileWriter(script));
                // Turn on/off verbose output
                 output.write("@echo off\n");
                // Settings that would otherwise be done in grassXx.bat
                output.write("set HOME=" + System.getProperty("user.home") + "\n");
                output.write("set GISRC=" + gisrc.getAbsolutePath() + "\n");
                output.write("set GRASS_SH=" + shell + "\n");
                if (shToolsPath!=null) {
                    output.write("set PATH=" + shToolsPath + File.separator + "bin;" + shToolsPath + File.separator + "lib;"
                            + "%PATH%\n");
                }
                output.write("set WINGISBASE=" + gisBase + "\n");
                output.write("set GISBASE=" + gisBase + "\n");
                output.write("set GRASS_PROJSHARE=" + gisBase + File.separator + "share" + File.separator + "proj" + "\n");
                output.write("set GRASS_MESSAGE_FORMAT=text\n");
                // Replacement code for etc/Init.bat
                output.write("if \"%GRASS_ADDON_PATH%\"==\"\" set PATH=%WINGISBASE%\\bin;%WINGISBASE%\\lib;%PATH%\n");
                output.write("if not \"%GRASS_ADDON_PATH%\"==\"\" set PATH=%WINGISBASE%\\bin;%WINGISBASE%\\lib;%GRASS_ADDON_PATH%;%PATH%\n");
                output.write("\n");
                output.write("if not \"%LANG%\"==\"\" goto langset\n");
                output.write("FOR /F \"usebackq delims==\" %%i IN (`\"%WINGISBASE%\\etc\\winlocale\"`) DO @set LANG=%%i\n");
                output.write(":langset\n");
                output.write("\n");
                output.write("set PATHEXT=%PATHEXT%;.PY\n");
                output.write("set PYTHONPATH=%PYTHONPATH%;%WINGISBASE%\\etc\\python;%WINGISBASE%\\etc\\wxpython\\n");
                output.write("\n");
                output.write("g.gisenv.exe set=\"MAPSET=" + mapset + "\"\n");
                output.write("g.gisenv.exe set=\"LOCATION=" + location + "\"\n");
                output.write("g.gisenv.exe set=\"LOCATION_NAME=" + location + "\"\n");
                output.write("g.gisenv.exe set=\"GISDBASE=" + gisdbase + "\"\n");
                output.write("g.gisenv.exe set=\"GRASS_GUI=text\"\n");
                output.write("\n");
                output.write("\n");
                output.write(grassCommand + "\n");
                output.write("\n");
                output.close();
            } catch (final Exception e) {
                return null;
            }
        }

        if (script == null || !script.exists()) {
            return null;
        }
        return script;
    }

}
