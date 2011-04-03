/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import java.util.Map;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** ProcessExcecution. Helper class to execute external programs.
 *
 * @author Olaf David
 * @author (Ant)
 */
public class Processes {

    public static final int INVALID_EXIT = Integer.MAX_VALUE;
    private ProcessBuilder pb = new ProcessBuilder();
    private int exitValue = INVALID_EXIT;
    private OutputStream out = System.out;
    private OutputStream err = System.err;
    private InputStream inp = System.in;
    private File executable;
    private Object[] args = new Object[]{};
    //
    boolean verbose;
    Process process;

    /** Create a new ProcessExecution.
     *
     * @param executable the executable file.
     */
    public Processes(File executable) {
        this.executable = executable;
    }
  
    /** Set the execution arguments.
     *
     * @param args the command line arguments
     */
    public void setArguments(Object... args) {
        this.args = args;
    }

    /** Set the working directory where the process get executed.
     *
     * @param dir the directory in which the executable will be started
     */
    public void setWorkingDirectory(File dir) {
        if (!dir.exists()) {
            throw new IllegalArgumentException(dir + " doesn't exist.");
        }
        pb.directory(dir);
    }

    /** get the execution environment. Use the returned map to customize
     * the environment variables.
     *
     * @return the process environment.
     */
    public Map<String, String> environment() {
        return pb.environment();
    }

    /** Redirect the output.
     *
     * @param out the new output stream
     */
    public void redirectOutput(OutputStream out) {
        this.out = out;
    }

    /** Redirect the error stream.
     *
     * @param err the new error stream
     */
    public void redirectError(OutputStream err) {
        this.err = err;
    }

    /** Redirect the input.
     *
     * @param inp the new input stream
     */
    public void redirectInput(InputStream inp) {
        this.inp = inp;
    }

    /** Get the exit value of the process.
     *
     * @return the exit value or INVALID_EXITVALUE if no exit value has
     * been received.
     */
    public int getExitValue() {
        return exitValue;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /** Process execution. This call blocks until the process is done.
     *
     * @throws java.lang.Exception
     * @return the exit status of the process.
     */
    public int exec() throws Exception {
        final List<String> argl = new ArrayList<String>();
        argl.add(executable.toString());
        for (Object a : args) {
            if (a != null) {
                if (a.getClass() == String.class) {
                    argl.add(a.toString());
                } else if (a.getClass() == String[].class) {
                    String[] f = (String[]) a;
                    for (String s : f) {
                        if (s != null && !s.isEmpty()) {
                            argl.add(s);
                        }
                    }
                }
            }
        }
        pb.command(argl);
        if (verbose) {
            System.out.println(pb.command().toString());
            // return 0;
        }

        final PumpStreamHandler streamHandler = new PumpStreamHandler(out, err, inp);
        process = pb.start();
        
        try {
           // streamHandler.setProcessInputStream(process.getOutputStream());
            streamHandler.setProcessOutputStream(process.getInputStream());
            streamHandler.setProcessErrorStream(process.getErrorStream());
        } catch (Exception e) {
            process.destroy();
            throw e;
        }

        streamHandler.start();

        try {
            // add the process to the list of those to destroy if the VM exits
//            processDestroyer.add(process);
//            if (watchdog != null) {
//                watchdog.start(process);
//            }

            waitFor(process);

//            if (watchdog != null) {
//                watchdog.stop();
//            }
            streamHandler.stop();
            closeStreams(process);

//            if (watchdog != null) {
//                watchdog.checkException();
//            }
            return getExitValue();
        } catch (ThreadDeath t) {
            // #31928: forcibly kill it before continuing.
            process.destroy();
            throw t;
        } finally {
            // remove the process to the list of those to destroy if
            // the VM exits
//          processDestroyer.remove(process);
        }
    }

    private static void closeStreams(Process process) {
        try {
            process.getInputStream().close();
            process.getOutputStream().close();
            process.getErrorStream().close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void waitFor(Process process) {
        try {
            process.waitFor();
            exitValue = process.exitValue();
        } catch (InterruptedException e) {
            process.destroy();
        }
    }

    private void destroy() {
        process.destroy();
    }

    /**
     * Copies standard output and error of subprocesses to standard output and
     * error of the parent process.
     *
     * @since Ant 1.2
     */
    private static class PumpStreamHandler {

        private Thread outputThread;
        private Thread errorThread;
        private StreamPumper inputPump;
        private OutputStream out;
        private OutputStream err;
        private InputStream input;

        /**
         * Construct a new <code>PumpStreamHandler</code>.
         * @param out the output <code>OutputStream</code>.
         * @param err the error <code>OutputStream</code>.
         * @param input the input <code>InputStream</code>.
         */
        PumpStreamHandler(OutputStream out, OutputStream err,
                InputStream input) {
            this.out = out;
            this.err = err;
            this.input = input;
        }

        /**
         * Construct a new <code>PumpStreamHandler</code>.
         * @param out the output <code>OutputStream</code>.
         * @param err the error <code>OutputStream</code>.
         */
        PumpStreamHandler(OutputStream out, OutputStream err) {
            this(out, err, null);
        }

        /**
         * Construct a new <code>PumpStreamHandler</code>.
         * @param outAndErr the output/error <code>OutputStream</code>.
         */
        PumpStreamHandler(OutputStream outAndErr) {
            this(outAndErr, outAndErr);
        }

        /**
         * Construct a new <code>PumpStreamHandler</code>.
         */
        PumpStreamHandler() {
            this(System.out, System.err);
        }

        /**
         * Set the <code>InputStream</code> from which to read the
         * standard output of the process.
         * @param is the <code>InputStream</code>.
         */
        void setProcessOutputStream(InputStream is) {
            createProcessOutputPump(is, out);
        }

        /**
         * Set the <code>InputStream</code> from which to read the
         * standard error of the process.
         * @param is the <code>InputStream</code>.
         */
        void setProcessErrorStream(InputStream is) {
            if (err != null) {
                createProcessErrorPump(is, err);
            }
        }

        /**
         * Set the <code>OutputStream</code> by means of which
         * input can be sent to the process.
         * @param os the <code>OutputStream</code>.
         */
        void setProcessInputStream(OutputStream os) {
            if (input != null) {
                inputPump = createInputPump(input, os, true);
            } else {
                try {
                    os.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }

        /**
         * Start the <code>Thread</code>s.
         */
        void start() {
            outputThread.start();
            errorThread.start();
            if (inputPump != null) {
                Thread inputThread = new Thread(inputPump);
                inputThread.setDaemon(true);
                inputThread.start();
            }
        }

        /**
         * Stop pumping the streams.
         */
        void stop() {
            try {
                outputThread.join();
            } catch (InterruptedException e) {
                // ignore
            }
            try {
                errorThread.join();
            } catch (InterruptedException e) {
                // ignore
            }

            if (inputPump != null) {
                inputPump.stop();
            }

            try {
                err.flush();
            } catch (IOException e) {
                // ignore
            }
            try {
                out.flush();
            } catch (IOException e) {
                // ignore
            }
        }

        /**
         * Get the error stream.
         * @return <code>OutputStream</code>.
         */
        OutputStream getErr() {
            return err;
        }

        /**
         * Get the output stream.
         * @return <code>OutputStream</code>.
         */
        OutputStream getOut() {
            return out;
        }

        /**
         * Create the pump to handle process output.
         * @param is the <code>InputStream</code>.
         * @param os the <code>OutputStream</code>.
         */
        void createProcessOutputPump(InputStream is, OutputStream os) {
            outputThread = createPump(is, os);
        }

        /**
         * Create the pump to handle error output.
         * @param is the input stream to copy from.
         * @param os the output stream to copy to.
         */
        void createProcessErrorPump(InputStream is, OutputStream os) {
            errorThread = createPump(is, os);
        }

        /**
         * Creates a stream pumper to copy the given input stream to the
         * given output stream.
         * @param is the input stream to copy from.
         * @param os the output stream to copy to.
         * @return a thread object that does the pumping.
         */
        Thread createPump(InputStream is, OutputStream os) {
            return createPump(is, os, false);
        }

        /**
         * Creates a stream pumper to copy the given input stream to the
         * given output stream.
         * @param is the input stream to copy from.
         * @param os the output stream to copy to.
         * @param closeWhenExhausted if true close the inputstream.
         * @return a thread object that does the pumping.
         */
        Thread createPump(InputStream is, OutputStream os,
                boolean closeWhenExhausted) {
            final Thread result = new Thread(new StreamPumper(is, os, closeWhenExhausted));
            result.setDaemon(true);
            return result;
        }

        /**
         * Creates a stream pumper to copy the given input stream to the
         * given output stream. Used for standard input.
         * @since Ant 1.6.3
         */
        StreamPumper createInputPump(InputStream is, OutputStream os,
                boolean closeWhenExhausted) {
            StreamPumper pumper = new StreamPumper(is, os, closeWhenExhausted);
            pumper.setAutoflush(true);
            return pumper;
        }
    }

    /**
     * Copies all data from an input stream to an output stream.
     *
     * @since Ant 1.2
     */
    private static class StreamPumper implements Runnable {

        private InputStream is;
        private OutputStream os;
        private volatile boolean finish;
        private volatile boolean finished;
        private boolean closeWhenExhausted;
        private boolean autoflush = false;
        private Exception exception = null;
        private int bufferSize = 128;
        private boolean started = false;

        /**
         * Create a new stream pumper.
         *
         * @param is input stream to read data from
         * @param os output stream to write data to.
         * @param closeWhenExhausted if true, the output stream will be closed when
         *        the input is exhausted.
         */
        StreamPumper(InputStream is, OutputStream os, boolean closeWhenExhausted) {
            this.is = is;
            this.os = os;
            this.closeWhenExhausted = closeWhenExhausted;
        }

        /**
         * Create a new stream pumper.
         *
         * @param is input stream to read data from
         * @param os output stream to write data to.
         */
        StreamPumper(InputStream is, OutputStream os) {
            this(is, os, false);
        }

        /**
         * Set whether data should be flushed through to the output stream.
         * @param autoflush if true, push through data; if false, let it be buffered
         * @since Ant 1.6.3
         */
        void setAutoflush(boolean autoflush) {
            this.autoflush = autoflush;
        }

        /**
         * Copies data from the input stream to the output stream.
         *
         * Terminates as soon as the input stream is closed or an error occurs.
         */
        public void run() {
            synchronized (this) {
                started = true;
            }
            finished = false;
            finish = false;

            final byte[] buf = new byte[bufferSize];

            int length;
            try {
                while ((length = is.read(buf)) > 0 && !finish) {
                    os.write(buf, 0, length);
                    if (autoflush) {
                        os.flush();
                    }
                }
                os.flush();
            } catch (Exception e) {
                synchronized (this) {
                    exception = e;
                }
            } finally {
                if (closeWhenExhausted) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                finished = true;
                synchronized (this) {
                    notifyAll();
                }
            }
        }

        /**
         * Tells whether the end of the stream has been reached.
         * @return true is the stream has been exhausted.
         */
        boolean isFinished() {
            return finished;
        }

        /**
         * This method blocks until the stream pumper finishes.
         * @throws InterruptedException if interrupted.
         * @see #isFinished()
         */
        synchronized void waitFor() throws InterruptedException {
            while (!isFinished()) {
                wait();
            }
        }

        /**
         * Set the size in bytes of the read buffer.
         * @param bufferSize the buffer size to use.
         * @throws IllegalStateException if the StreamPumper is already running.
         */
        synchronized void setBufferSize(int bufferSize) {
            if (started) {
                throw new IllegalStateException("Cannot set buffer size on a running StreamPumper");
            }
            this.bufferSize = bufferSize;
        }

        /**
         * Get the size in bytes of the read buffer.
         * @return the int size of the read buffer.
         */
        synchronized int getBufferSize() {
            return bufferSize;
        }

        /**
         * Get the exception encountered, if any.
         * @return the Exception encountered.
         */
        synchronized Exception getException() {
            return exception;
        }

        /**
         * Stop the pumper as soon as possible.
         * Note that it may continue to block on the input stream
         * but it will really stop the thread as soon as it gets EOF
         * or any byte, and it will be marked as finished.
         * @since Ant 1.6.3
         */
        synchronized void stop() {
            finish = true;
            notifyAll();
        }
    }

    public static void main(String[] args) throws Exception {
//        File javaExe = new File(System.getProperty("java.home") + "/bin/java");
//        System.out.println(javaExe);
//        Processes p = new Processes(javaExe);
//        p.setWorkingDirectory(new File("c:/od/oms/work21/prms/dist/models"));
//        p.setArguments("-jar", "EFCarson.jar", "-Pparams.csp");
//        int exitValue = p.exec();
//        System.out.println("Exit with " + exitValue);

        final Processes p = new Processes(new File("wine"));
        p.setVerbose(true);
//        p.setWorkingDirectory(new File("c:/od/oms/work21/prms/dist/models"));
//        p.setArguments("-Dwork=\"c:/od/projects/oms3.prj.prms2008\"", "-jar", "/od/projects/ngmf.all/lib/oms-all.jar",  "-r", "/od/projects/oms3.prj.prms2008/simulation/efcarson/efc.luca");
//        p.setArguments("-Doms3.work=/od/projects/oms3.prj.prms2008", "-jar", "/od/projects/ngmf.all/lib/oms-all.jar",  "-r", "/od/projects/oms3.prj.prms2008/simulation/efcarson/efc.sim");
        p.setArguments("/od/software/CPD-Rusle/RomeConsole SDK 2010-03-31/Release/RomeConsole.exe");

        final StringBuffer a = new StringBuffer();
        p.redirectOutput(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                a.append((char)b);
            }
        });
        p.redirectError(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
//                a.append((char)b);
            }
        });


       
//        SwingUtilities.invokeLater(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(5000);
//                    p.destroy();
//                } catch (InterruptedException ex) {
//                    Logger.getLogger(Processes.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
//        });
        int exitValue = p.exec();
        System.out.println("Exit with " + exitValue);
        System.out.println(a.substring(a.indexOf("(null) = ") + 9, a.lastIndexOf("(null) = ")).trim());
        System.out.println(a.substring(a.lastIndexOf("(null) = ") + 9).trim());

//        System.out.println(a);

//        Processes p = new Processes(new File("c:/projects/rz/RZWQMrelease.exe"));
//        p.setWorkingDirectory(new File("C:/projects/rz/"));
//        int exitValue =  p.exec();
//        System.out.println("Exit with " + exitValue);
    }
}
