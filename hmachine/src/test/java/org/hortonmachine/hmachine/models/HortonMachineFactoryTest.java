package org.hortonmachine.hmachine.models;
//package org.hortonmachine.hmachine.models;
//
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import org.geotools.data.Parameter;
//import org.geotools.feature.NameImpl;
//import org.geotools.process.Process;
//import org.geotools.process.ProcessExecutor;
//import org.geotools.process.ProcessFactory;
//import org.geotools.process.Processors;
//import org.geotools.process.Progress;
//import org.junit.Test;
//import org.opengis.feature.type.Name;
//
///**
// * Time to check that GearsProcessFactry meets the process api.
// * <p>
// * To help with this we will use the Processors class for all interaction; and
// * only break out specific test cases as issues are found.
// * 
// * @author Jody
// */
//public class HortonMachineFactoryTest {
//
//    @Test
//    public void testOne() throws Exception {
//        Name NAME = new NameImpl("org.hortonmachine.hmachine", "OmsGradient");
//        ProcessFactory factory = Processors.createProcessFactory(NAME);
//        assertNotNull(factory);
//        assertTrue(factory.isAvailable());
//
//        // InternationalString description = factory.getDescription(NAME);
//        // assertNotNull( description );
//
//        String version = factory.getVersion(NAME);
//        assertNotNull(version);
//
//        Map<String, Parameter< ? >> info = factory.getParameterInfo(NAME);
//        assertNotNull(info);
//        assertTrue(info.containsKey("inElev"));
//        // assertFalse( info.containsKey("pm")); // I cannot ask users to type
//        // this in
//
//        ProcessExecutor pool = Processors.newProcessExecutor(3, null);
//        Process grad = Processors.createProcess(NAME);
//
//        Map<String, Object> input = new HashMap<String, Object>();
//        Progress progress = pool.submit(grad, input);
//
//        // float work = progress.getProgress();
//        Map<String, Object> result = progress.get();
//
//        assertTrue(progress.isDone());
//        assertNotNull(result);
//        assertTrue(result.containsKey("outSlope"));
//
//    }
//}
