//package oms3.dsl;
//
//import groovy.lang.Closure;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.TimeoutException;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import oms3.Access;
//import oms3.ComponentAccess;
//import oms3.ComponentException;
//import oms3.annotations.*;
//
//public class Tests extends AbstractSimulation {
//
//    static final Logger log = Logger.getLogger("oms3.sim");
//    List<TestCase> testCases = new ArrayList<TestCase>();
//
//    class TestCase implements Buildable {
//
//        String name;
//        int count = 1;
//        Closure pre;     // Closure
//        Closure post;    // Closure
//        String ignore = null;
//        Throwable expected;
//        long timeout = 0;
//        boolean rangecheck = false;
//        List data;
//
//        public void setData(List data) {
//            this.data = data;
//        }
//
//        public void setTimeout(long timeout) {
//            if (timeout < 1) {
//                throw new ComponentException("Illegal timeout value: " + timeout);
//            }
//            this.timeout = timeout;
//        }
//
//        public void setRangecheck(boolean rangecheck) {
//            this.rangecheck = rangecheck;
//        }
//
//        public void setIgnore(String ignore) {
//            this.ignore = ignore;
//        }
//
//        public void setExpected(Throwable expected) {
//            this.expected = expected;
//        }
//
//        public void setName(String name) {
//            this.name = name;
//        }
//
//        public void setCount(int count) {
//            if (count < 1) {
//                throw new ComponentException("Illegal number of test cases: " + count);
//            }
//            this.count = count;
//        }
//
//        public void setPre(Closure pre) {
//            this.pre = pre;
//        }
//
//        public void setPost(Closure post) {
//            this.post = post;
//        }
//
//        @Override
//        public Buildable create(Object name, Object value) {
//            return LEAF;
//        }
//
//        private String getTestName(int no) {
//            return name != null ? name : ("test-" + no);
//        }
//
//        /**
//         * 
//         * @param testNo
//         * @throws Exception 
//         */
//        private void run(int testNo) throws Exception {
//            if (ignore!=null) {
//                System.out.println(getTestName(testNo) + " [ignored: " + ignore + "]");
//                return;
//            }
//
//           
//            System.out.print(getTestName(testNo) + " ");
//
//            // Path
//            String libPath = model.getLibpath();
//            if (libPath != null) {
//                System.setProperty("jna.library.path", libPath);
//                if (log.isLoggable(Level.CONFIG)) {
//                    log.config("Setting jna.library.path to " + libPath);
//                }
//            }
//
//            final Object comp = model.getComponent();
//            if (log.isLoggable(Level.CONFIG)) {
//                log.config("TL component " + comp);
//            }
//
//            Map<String, Object> parameter = model.getParameter();
//            ComponentAccess.setInputData(parameter, comp, log);
//
//            List<String> data_ins = null;
//            Map<String, Object>[] ps = null;
//
//            int numruns = count;
//
//            if (data != null) {
//                data_ins = getfromData(data, comp);
//                numruns = data.size() / data_ins.size() - 1;
//                ps = getParamsets(data_ins, data);
//            }
//
//            for (int i = 0; i < numruns; i++) {
//                if (log.isLoggable(Level.INFO)) {
//                    log.info("Test ... " + i);
//                }
//
//                try {
//                    if (data != null) {
//                        ComponentAccess.setInputData(ps[i], comp, log);
//                    }
//
//                    if (pre != null) {
//                        if (log.isLoggable(Level.INFO)) {
//                            log.info(" Pre ...");
//                        }
//                        pre.call(comp);
//                    }
//                    /////////////// Init
//                    if (log.isLoggable(Level.INFO)) {
//                        log.info(" Init ...");
//                    }
//                    ComponentAccess.callAnnotated(comp, Initialize.class, true);
//
//                    if (rangecheck) {
//                        ComponentAccess.rangeCheck(comp, true, false);
//                    }
//
//                    ///////////////  Execute 
//                    if (log.isLoggable(Level.INFO)) {
//                        log.info(" Execute ...");
//                    }
//                    if (timeout > 0) {
//                        // with execution timeout
//                        ExecutorService service = Executors.newSingleThreadExecutor();
//                        Callable<Object> callable = new Callable<Object>() {
//
//                            @Override
//                            public Object call() throws Exception {
//                                ComponentAccess.callAnnotated(comp, Execute.class, false);
//                                return null;
//                            }
//                        };
//                        Future<Object> result = service.submit(callable);
//                        service.shutdown();
//                        boolean terminated = service.awaitTermination(timeout, TimeUnit.MILLISECONDS);
//                        if (!terminated) {
//                            service.shutdownNow();
//                        }
//                        result.get(0, TimeUnit.MILLISECONDS); // throws the exception if one occurred during the invocation
//                    } else {
//                        // no execution timeout.
//                        ComponentAccess.callAnnotated(comp, Execute.class, false);
//                    }
//
//                    if (rangecheck) {
//                        ComponentAccess.rangeCheck(comp, false, true);
//                    }
//
//                    /////////////////// Finalize
//                    if (log.isLoggable(Level.INFO)) {
//                        log.info(" Finalize ...");
//                    }
//                    ComponentAccess.callAnnotated(comp, Finalize.class, true);
//
//                } catch (TimeoutException e) {
//                    throw new ComponentException(String.format(getTestName(testNo) + " timed out after %d milliseconds", timeout));
//                } catch (ComponentException e) {
//                    throw e;
//                } catch (Throwable E) {
//                    if (expected != null) {
//                        if (E.getClass() != expected.getClass()) {
//                            throw new ComponentException("Expected " + expected + " but caught " + E);
//                        }
//                    } else {
//                        throw new ComponentException(E, comp);
//                    }
//                }
//
//                if (expected != null) {
//                    // should not happen since we expect a component exception.
//                    throw new ComponentException("Expected :" + expected);
//                }
//
//                if (post != null) {
//                    if (log.isLoggable(Level.INFO)) {
//                        log.info(" Post ...");
//                    }
//                    post.call(comp);
//                }
//                System.out.print(".");
//            }
//            System.out.println();
//        }
//    }
//
//    @Override
//    public Buildable create(Object name, Object value) {
//        if (name.equals("test")) {
//            TestCase tc = new TestCase();
//            testCases.add(tc);
//            return tc;
//        }
//        return super.create(name, value);
//    }
//
//    @Override
//    public Object run() throws Exception {
//        if (testCases.isEmpty()) {
//            throw new ComponentException("No test(s) to run.");
//        }
//        for (int i = 0; i < testCases.size(); i++) {
//            testCases.get(i).run(i);
//        }
//        return null;
//    }
//
//    // static methods
//    private static List<String> getfromData(List data, Object comp) {
//        List<String> l = new ArrayList<String>();
//        ComponentAccess cp = new ComponentAccess(comp);
//        Collection<Access> ins = cp.inputs();
//        for (int i = 0; i < data.size(); i++) {
//            if (data.get(i) instanceof String) {
//                String name = data.get(i).toString();  // cover GString
//                Access a = findAccessByName(name, ins);
//                if (a != null) {
//                    l.add(a.getField().getName());
//                } else {
//                    return l;
//                }
//            } else {
//                return l;
//            }
//        }
//        return l;
//    }
//
//    private static Access findAccessByName(String name, Collection<Access> ins) {
//        for (Access access : ins) {
//            if (name.equals(access.getField().getName())) {
//                return access;
//            }
//        }
//        return null;
//    }
//
//    private static Map<String, Object>[] getParamsets(List<String> fnames, List data) {
//        int cols = fnames.size();
//        if (cols<1) {
//            throw new ComponentException("no field mapping information for test data");
//        }
//        int rows = (data.size() / fnames.size()) - 1;
//        if (rows < 1) {
//            throw new ComponentException("no test data provided.");
//        }
//        int idx = cols;   // skip the header
//
//        Map<String, Object>[] ps = new HashMap[rows];
//        for (int r = 0; r < rows; r++) {
//            ps[r] = new HashMap<String, Object>();
//            for (int c = 0; c < cols; c++) {
//                ps[r].put(fnames.get(c), data.get(idx++));
//                if (idx > data.size()) {
//                    throw new ComponentException("Invalid test parameter set, check the data size.");
//                }
//            }
//        }
//        return ps;
//    }
//}
