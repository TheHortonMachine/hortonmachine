/*
 * $Id: ProcessExecution.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.util;

/*
 * ProcessExec.java
 *
 * Created on February 6, 2007, 8:52 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in th
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ProcessExcecution. Helper class to execute external programs.
 *
 * @author od
 */
public class ProcessExecution {

    ProcessBuilder pb = new ProcessBuilder();
    File executable;
    Object[] args = new Object[]{};
    Logger logger;
    //
    Writer stderr = new OutputStreamWriter(System.err) {
        @Override
        public void close() throws IOException {
            System.err.flush();
        }
    };
    //
    Writer stdout = new OutputStreamWriter(System.out) {
        @Override
        public void close() throws IOException {
            System.out.flush();
        }
    };

    /**
     * Null Writer for doing nothing.
     */
    public static class NullWriter extends Writer {

        @Override
        public void close() throws IOException {
        }


        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
        }


        @Override
        public void flush() throws IOException {
        }
    }


    /**
     * Create a new ProcessExecution.
     *
     * @param executable the executable file.
     */
    public ProcessExecution(File executable) {
        this.executable = executable;
    }


    /**
     * Set the execution arguments.
     *
     * @param args the command line arguments
     */
    public void setArguments(Object... args) {
        this.args = args;
    }


    /**
     * Set the execution arguments.
     *
     * @param args the program arguments.
     */
    public void setArguments(List<Object> args) {
        this.args = args.toArray(new Object[args.size()]);
    }


    /**
     * Set the working directory where the process get executed.
     *
     * @param dir the directory in which the executable will be started
     */
    public void setWorkingDirectory(File dir) {
        if (!dir.exists() || dir.isFile()) {
            throw new IllegalArgumentException("directory " + dir + " doesn't exist.");
        }
        pb.directory(dir);
    }


    /**
     * get the execution environment. Use the returned map to customize the
     * environment variables.
     *
     * @return the process environment.
     */
    public Map<String, String> environment() {
        return pb.environment();
    }


    /**
     * Set the logger. This is optional.
     *
     * @param log the logger
     */
    public void setLogger(Logger log) {
        this.logger = log;
    }

    private static class OutputHandler extends Thread {

        Writer w;
        Reader r;
        CountDownLatch l;


        OutputHandler(CountDownLatch l, InputStream is, Writer w) {
            r = new InputStreamReader(is);
            this.w = w;
            this.l = l;
           // setPriority(Thread.MIN_PRIORITY);
        }


        @Override
        public void run() {
            try {
                char[] b = new char[2048];
                int n = 0;
                while ((n = r.read(b)) != -1) {
                    w.write(b, 0, n);
                    w.flush();
                }
            } catch (IOException ex) {
                if (ex.getMessage().equals("Stream closed.")) {
                    return;
                }
                ex.printStackTrace(System.err);
            } finally {
                try {
                    w.flush();
                    w.close();
                    r.close();
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
                l.countDown();
            }
        }
    }


    /**
     * Process execution. This call blocks until the process is done.
     *
     * @return the exit status of the process.
     * @throws IOException I/O exception occurred
     */
    public int exec() throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(executable.toString());
        for (Object arg : args) {
            if (arg != null) {
                if (arg.getClass() == String[].class) {
                    for (String s : (String[]) arg) {
                        if (s != null && !s.isEmpty()) {
                            cmd.add(s);
                        }
                    }
                } else {
                    cmd.add(arg.toString());
                }
            }
        }
        pb.command(cmd);

        if (logger != null && logger.isLoggable(Level.INFO)) {
            logger.info(pb.command().toString());
        }

        CountDownLatch l = new CountDownLatch(2);

        Process p = pb.start();
        new OutputHandler(l, p.getInputStream(), stdout).start();
        new OutputHandler(l, p.getErrorStream(), stderr).start();

        int exitValue = 0;
        try {
            exitValue = p.waitFor();
            l.await();
        } catch (InterruptedException E) {
            // do nothing
        } finally {
            p.getOutputStream().close();
            p.destroy();
        }
        return exitValue;
    }


    /**
     * Redirect the output stream
     *
     * @param w the stream handler
     */
    public void redirectOutput(Writer w) {
        if (w == null) {
            throw new NullPointerException("w");
        }
        stdout = w;
    }


    /**
     * Redirect the error stream
     *
     * @param w the new handler.
     */
    public void redirectError(Writer w) {
        if (w == null) {
            throw new NullPointerException("w");
        }
        stderr = w;
    }


    public static void main(String[] args) throws Exception {
//        ProcessExecution p = new ProcessExecution(new File("bash"));
//        p.setArguments("-c", "wine ./RZWQMRelease.exe 1>stdout.txt 2>stderr.txt");
        run(
                new Runnable() {

                    @Override
                    public void run() throws RuntimeException {
                        try {
                            System.out.println("1 ...");
                            ProcessExecution p = new ProcessExecution(new File("/usr/bin/wine"));
                            p.setArguments("/od/software/RZWQM2_Ver25_Intel/SampleData/RZWQMRelease.exe");
                            p.setWorkingDirectory(new File("/od/software/RZWQM2_Ver25_Intel/SampleData"));
                            p.redirectOutput(new FileWriter("/tmp/stdout1"));
                            p.redirectError(new FileWriter("/tmp/stderr1"));
                            int exitValue = p.exec();
                            System.out.println("1 ... done " + exitValue);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                },
                new Runnable() {

                    @Override
                    public void run() throws RuntimeException {
                        try {
                            System.out.println("2 ...");
                            ProcessExecution p = new ProcessExecution(new File("/usr/bin/wine"));
                            p.setArguments("/od/software/RZWQM2_Ver25_Intel/SampleData/RZWQMRelease.exe");
                            p.setWorkingDirectory(new File("/od/software/RZWQM2_Ver25_Intel/SampleData1"));
                            p.redirectOutput(new FileWriter("/tmp/stdout2"));
                            p.redirectError(new FileWriter("/tmp/stderr2"));
                            int exitValue = p.exec();
                            System.out.println("2 ... done" + exitValue);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                },
                new Runnable() {

                    @Override
                    public void run() throws RuntimeException {
                        try {
                            System.out.println("3 ...");
                            ProcessExecution p = new ProcessExecution(new File("/usr/bin/wine"));
                            p.setArguments("/od/software/RZWQM2_Ver25_Intel/SampleData/RZWQMRelease.exe");
                            p.setWorkingDirectory(new File("/od/software/RZWQM2_Ver25_Intel/SampleData2"));
                            p.redirectOutput(new FileWriter("/tmp/stdout3"));
                            p.redirectError(new FileWriter("/tmp/stderr3"));
                            int exitValue = p.exec();
                            System.out.println("3 ... done" + exitValue);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                },
                new Runnable() {

                    @Override
                    public void run() throws RuntimeException {
                        try {
                            System.out.println("4 ...");
                            ProcessExecution p = new ProcessExecution(new File("/usr/bin/wine"));
                            p.setArguments("/od/software/RZWQM2_Ver25_Intel/SampleData/RZWQMRelease.exe");
                            p.setWorkingDirectory(new File("/od/software/RZWQM2_Ver25_Intel/SampleData3"));
                            p.redirectOutput(new FileWriter("/tmp/stdout3"));
                            p.redirectError(new FileWriter("/tmp/stderr3"));
                            int exitValue = p.exec();
                            System.out.println("4 ... done" + exitValue);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
        );

    }


    public static void run(Runnable... r) {

        ExecutorService es = Executors.newFixedThreadPool(r.length);
        final CountDownLatch l = new CountDownLatch(r.length);

        for (final Runnable runnable : r) {
            es.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } finally {
                        l.countDown();
                    }
                }
            });

        }
        try {
            l.await();
        } catch (InterruptedException ex) {
        }
        es.shutdownNow();
    }
}
