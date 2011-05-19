package org.jgrasstools.grass.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class GrassRunner {

    public static String runModule( String[] cmdArgs, String gisbase, String gisrc ) throws Exception {
        final boolean[] outputDone = {false};
        final boolean[] errorDone = {false};

        ProcessBuilder processBuilder = new ProcessBuilder(cmdArgs);
        String homeDir = System.getProperty("java.home");
        processBuilder.directory(new File(homeDir));
        Map<String, String> environment = processBuilder.environment();

        environment.put("GISBASE", gisbase);
        environment.put("GISRC", gisrc);
        environment.put("LD_LIBRARY_PATH", gisbase + "/lib");
        environment.put("PATH", "$PATH:$GISBASE/bin:$GISBASE/scripts");

        final Process process = processBuilder.start();

        final StringBuffer outSb = new StringBuffer();
        final StringBuffer errSb = new StringBuffer();

        Thread outputThread = new Thread(){
            public void run() {
                try {
                    InputStream is = process.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String line;
                    while( (line = br.readLine()) != null ) {
                        outSb.append(line).append("\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errSb.append(e.getLocalizedMessage()).append("\n");
                } finally {
                    outputDone[0] = true;
                }
            };
        };

        Thread errorThread = new Thread(){
            public void run() {
                try {
                    InputStream is = process.getErrorStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String line;
                    while( (line = br.readLine()) != null ) {
                        outSb.append(line).append("\n");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errSb.append(e.getLocalizedMessage()).append("\n");
                } finally {
                    errorDone[0] = true;
                }
            };
        };

        outputThread.start();
        errorThread.start();

        while( !outputDone[0] && !errorDone[0] ) {
            Thread.sleep(100);
        }

        if (errSb.length() != 0) {
            return errSb.toString();
        } else {
            return outSb.toString();
        }
    }

    public static void main( String[] args ) throws Exception {

        String result = runModule(new String[]{"/usr/lib/grass64/bin/v.in.ascii", "--interface-description"}, "/usr/lib/grass64",
                "/tmp/grass6-moovida-10940/gisrc");

        System.out.println(result);
    }
}
