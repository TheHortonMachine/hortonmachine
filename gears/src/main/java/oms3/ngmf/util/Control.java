/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Cluster control shell.
 *
 * @author od
 */
public class Control {

    static final String SSH_KEY = "ssh";
    static final String KEY_KEY = "key";
    static final String SCP_KEY = "scp";
    static final String USER_KEY = "user";
    static final String CLUSTER_KEY = "cluster.";
//
    final File tempFolder = new File(System.getProperty("java.io.tmpdir"));
    final Properties conf = new Properties();
    String[] nodes;
    List<Integer> idx;
    boolean verbose = true;
    boolean sync;

    /*
     * key=c:/cygwin/home/od/odkey.pem
    ssh=c:/cygwin/bin/ssh
    scp=c:/cygwin/bin/scp
    cluster.default=localhost test
    cluster.ec2=ec2-174-129-140-138.compute-1.amazona...
     */
    private OutputStream getOutputStream(final String s) {
        return new OutputStream() {

            StringBuffer o = new StringBuffer();
            boolean done = false;

            @Override
            public void write(int b) throws IOException {
                if (b == '\n' && !sync) {
                    synchronized (conf) {
                        System.out.printf("[%s] %s \n", s, o.toString());
                    }
                    o = new StringBuffer();
                }
                o.append((char) b);
            }

            @Override
            public void flush() throws IOException {
                if (!done) {
                    if (sync) {
                        System.out.printf("************* %s **************\n", s);
                    }
                    System.out.printf(o.toString());
                    done = true;
                }
            }
        };
    }

    private String[] getNodes(String clusterName) {
        String s = conf.getProperty(CLUSTER_KEY + clusterName);
        if (s != null) {
            String[] n = s.split("\\s+");
            if (n.length < 1) {
                throw new IllegalArgumentException("No nodes in cluster " + clusterName);
            }
            return n;
        }
        throw new IllegalArgumentException("No such cluster '" + clusterName + "'");
    }

    private interface RCmd {

        File file();

        String[] args(String node);
    }

    public void runCExec(List<String> nodes, final String cmd) {
        run(nodes, new RCmd() {

            @Override
            public File file() {
                return new File(conf.getProperty(SSH_KEY));
            }

            @Override
            public String[] args(String node) {
                return new String[]{"-i", conf.getProperty(KEY_KEY), "-l", conf.getProperty(USER_KEY), node, cmd};
            }
        });
    }

    public void runCPut(List<String> nodes, final String from, final String to) {
        run(nodes, new RCmd() {

            @Override
            public File file() {
                return new File(conf.getProperty(SCP_KEY));
            }

            @Override
            public String[] args(String node) {
                return new String[]{"-i", conf.getProperty(KEY_KEY), from, conf.getProperty(USER_KEY) + '@' + node + ":" + to};
            }
        });
    }

    public void runCGet(List<String> nodes, final String from, final String to) {
        run(nodes, new RCmd() {

            @Override
            public File file() {
                return new File(conf.getProperty(SCP_KEY));
            }

            @Override
            public String[] args(String node) {
                return new String[]{"-i", conf.getProperty(KEY_KEY), conf.getProperty(USER_KEY) + '@' + node + ":" + from, to};
            }
        });
    }

    private void run(List<String> nodes, final RCmd rcmd) {
        final CountDownLatch latch = new CountDownLatch(idx.size());
        ExecutorService e = Executors.newFixedThreadPool(idx.size());

        final File userDir = new File(System.getProperty("user.dir"));
        for (final String node : nodes) {
            e.submit(new Runnable() {

                @Override
                public void run() {
                    try {
//                        MyProcesses p = new MyProcesses(rcmd.file());
//                                p.setArguments((Object)rcmd.args(node));
//                        p.setVerbose(verbose);
//                        OutputStream out = getOutputStream(node);
//                        p.redirectOutput(out);
//                        p.setWorkingDirectory(userDir);
//                        int exitValue = p.exec();
//                        synchronized (conf) {
//                            System.out.printf("DONE on %s, exit code %d.\n", node, exitValue);
//                        }
                    } catch (Throwable E) {
                        E.printStackTrace();
                    }
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            e.shutdown();
        }
    }

    public Control() throws IOException {
        InputStream is = new FileInputStream(new File(System.getProperty("user.home"), "nc2.conf"));
        conf.load(is);
        is.close();
        if (!(conf.containsKey(SSH_KEY) && conf.containsKey(KEY_KEY) && conf.containsKey(SCP_KEY) &&
                conf.containsKey(USER_KEY) && conf.containsKey("cluster.default"))) {
            throw new IllegalArgumentException("Missing entries in nc2.conf");
        }
        conf.list(System.out);
        nodes = getNodes("default");
//        System.out.println(Arrays.toString(nodes));
//        conf.list(System.out);
        idx = parseRange("0-*", nodes.length);
        System.out.printf(" NGMF Cluster Control v0.1\n\n");
    }

    public void process(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-exec")) {
                String cmd = "";
                for (i = i + 1; i < args.length; i++) {
                    cmd = cmd + " " + args[i];
                }
//                runCExec(nodes, cmd);
            } else if (arg.startsWith("-put")) {
//                runCPut(nodes, args[++i], args[++i]);
            } else if (arg.equals("-list")) {
                for (int j = 0; j < nodes.length; j++) {
                    System.out.printf(" %0$5d: %s\n", j, nodes[j]);
                }
            } else if (arg.equals("-s")) {
                sync = true;
            } else if (arg.equals("-v")) {
                verbose = true;
            } else if (arg.equals("-c")) {
                String cluster = args[++i];
                String[] cl = cluster.split(":");
                nodes = getNodes(cl[0]);
                idx = parseRange(cl.length == 2 ? cl[1] : "0-*", nodes.length);
            } else {
                System.err.printf(" Invalid Option '%s'.\n", arg);
                System.exit(0);
            }
        }
    }

//  Integer[] idx = rangeparser("1,3-5,6,7-*",20);
    private static List<Integer> parseRange(String range, int max) {
        List<Integer> idx = new ArrayList<Integer>();
        String[] n = range.split(",");
        for (String s : n) {
            String[] d = s.split("-");
            int mi = Integer.parseInt(d[0]);
            if (mi < 0 || mi >= max) {
                throw new IllegalArgumentException(range);
            }
            if (d.length == 2) {
                if (d[1].equals("*")) {
                    d[1] = Integer.toString(max - 1);
                }
                int ma = Integer.parseInt(d[1]);
                if (ma <= mi || ma >= max || ma < 0) {
                    throw new IllegalArgumentException(range);
                }
                for (int i = mi; i <= ma; i++) {
                    idx.add(i);
                }
            } else {
                idx.add(mi);
            }
        }
        return idx;
    }

    private static List<String> ips(List<Integer> a, List<Integer> b, List<Integer> c, List<Integer> d) {
        List<String> ip = new ArrayList<String>();
        for (Integer ai : a) {
            for (Integer bi : b) {
                for (Integer ci : c) {
                    for (Integer di : d) {
                        ip.add(ai + "." + bi + "." + ci + "." + di);
                    }
                }
            }
        }
        return ip;
    }

    /**
     * -v -c ec2:1-4 -exec ls -l
     * @param args
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
//        Control c = new Control();
//        c.process(args);
        String cmd = "test jsjs {1} {0} ksksks {2}";
        System.out.println(MessageFormat.format(cmd, 1, 2, "test"));
    }
}
