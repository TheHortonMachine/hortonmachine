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
package org.hortonmachine.llm.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Tests the {@link ScriptGuard} on the modules of the classpath.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestScriptGuard {

    private static ScriptGuard.Result check( String script ) {
        return ScriptGuard.check(script, ModuleCatalog.getDefault());
    }

    @Test
    public void testGoodScript() throws Exception {
        ScriptGuard.Result result = check("""
                import org.hortonmachine.modules.*

                def dtm = "/data/dtm.tif"
                def pitfiller = new Pitfiller()
                pitfiller.inElev = dtm
                pitfiller.outPit = "/data/pit.tif"
                pitfiller.process()

                Pitfiller other = new Pitfiller()
                other.with {
                    inElev = dtm
                    outPit = "/data/pit2.tif"
                }
                other.process()

                def basin = new org.hortonmachine.modules.ExtractBasin()
                basin.inFlow = "/data/flow.tif"
                basin.process()
                println basin.outArea
                """);
        assertEquals(List.of(), result.errors());
        assertEquals(List.of("Pitfiller", "ExtractBasin"), result.modules());
    }

    @Test
    public void testWrongFields() throws Exception {
        ScriptGuard.Result result = check("""
                def pit = new Pitfiller()
                pit.inElevation = "/data/dtm.tif"
                pit.outPit = "/data/pit.tif"
                pit.run()
                def basin = new ExtractBasin()
                basin.outArea = 10
                """);
        List<String> errors = result.errors();
        assertEquals(errors.toString(), 5, errors.size());
        assertEquals("The module Pitfiller has no field inElevation. Its fields are: inElev, outPit.", errors.get(0));
        assertEquals("The module Pitfiller has no method run(): run it with pit.process().", errors.get(1));
        assertTrue(errors.get(2), errors.get(2).startsWith("The field outArea of ExtractBasin is a result of the module"));
        assertEquals("The module Pitfiller in pit is never run: call pit.process() after setting its fields.", errors.get(3));
        assertEquals("The module ExtractBasin in basin is never run: call basin.process() after setting its fields.",
                errors.get(4));
    }

    @Test
    public void testNotRun() throws Exception {
        ScriptGuard.Result result = check("""
                def pit = new Pitfiller()
                pit.inElev = "/data/dtm.tif"
                """);
        assertEquals(List.of("The module Pitfiller in pit is never run: call pit.process() after setting its fields."),
                result.errors());
    }

    @Test
    public void testCommentsAndStringsIgnored() throws Exception {
        ScriptGuard.Result result = check("""
                def pit = new Pitfiller()
                // pit.wrongInComment = 1
                /* pit.wrongInBlock = 2
                   still a comment */
                pit.inElev = "pit.wrongInString"
                pit.outPit = '''pit.wrong
                    in a multiline string'''
                println "${pit.wrongInGString}"
                pit.process()
                """);
        assertEquals(List.of(), result.errors());
    }

    @Test
    public void testStrip() throws Exception {
        String code = ScriptGuard.stripCommentsAndStrings("a = \"x.y\" // c\nb = 'it\\'s' /* d\ne */ f");
        assertEquals("a = \"\" \nb = '' \n f", code);
        // an unclosed string ends with its line
        assertEquals("a = \"\nb.c = 1", ScriptGuard.stripCommentsAndStrings("a = \"open\nb.c = 1"));
    }

    @Test
    public void testOtherClassesIgnored() throws Exception {
        ScriptGuard.Result result = check("""
                def list = new ArrayList()
                list.whatever = 1
                def pit = new Pitfiller()
                pit = new ArrayList()
                pit.notAField = 2
                """);
        assertEquals(List.of(), result.errors());
        assertEquals(List.of("Pitfiller"), result.modules());
    }

    @Test
    public void testKindsOfAGoodChain() throws Exception {
        // outputs passed on directly, through variables and through the same path
        ScriptGuard.Result result = check("""
                import org.hortonmachine.modules.*
                def dem = "/data/dem.tif"
                def pitPath = "/data/out.tif"
                def pitfiller = new Pitfiller()
                pitfiller.inElev = dem
                pitfiller.outPit = pitPath
                pitfiller.process()
                def flowdirections = new FlowDirections()
                flowdirections.inPit = pitPath // the kind of the output that writes it
                flowdirections.outFlow = "/data/drain.tif"
                flowdirections.process()
                def tca = new Tca()
                tca.inFlow = flowdirections.outFlow
                tca.outTca = "/data/tca.tif"
                tca.process()
                def basin = new ExtractBasin()
                basin.inFlow = "/data/drain.tif"
                basin.outBasin = "/data/basin.tif"
                basin.process()
                // a depitted dem is an elevation model too, unknown names say nothing
                def slope = new Slope()
                slope.inPit = pitfiller.outPit
                slope.inFlow = folder + "/x.tif"
                slope.outSlope = "/data/slope.tif"
                slope.process()
                def other = new Pitfiller()
                other.inElev = pitfiller.outPit
                other.outPit = "/data/pit2.tif"
                other.process()
                """);
        assertEquals(List.of(), result.errors());
    }

    @Test
    public void testKindsOfAWrongChain() throws Exception {
        // the answer that started the kinds: the dem given as drainage directions
        ScriptGuard.Result result = check("""
                import org.hortonmachine.modules.*
                def demPath = "/path/to/your/dem.tif"
                def extractBasin = new ExtractBasin()
                extractBasin.inFlow = demPath
                extractBasin.outBasin = "/path/to/output/basin_mask.tif"
                extractBasin.process()
                """);
        assertEquals(1, result.errors().size());
        String error = result.errors().get(0);
        assertTrue(error, error.startsWith("The field inFlow of ExtractBasin reads the D8 drainage directions"));
        assertTrue(error, error.contains("but gets demPath, which is Elev."));
        assertTrue(error, error.contains("Give it the output of FlowDirections.outFlow, DrainDir.outFlow"));

        // an output of the wrong kind, and a path written by an output of the wrong kind
        result = check("""
                import org.hortonmachine.modules.*
                def pitfiller = new Pitfiller()
                pitfiller.inElev = "/data/dtm.tif"
                pitfiller.outPit = "/data/a.tif"
                pitfiller.process()
                def tca = new Tca()
                tca.inFlow = pitfiller.outPit
                tca.outTca = "/data/tca.tif"
                tca.process()
                def network = new ExtractNetwork()
                network.inTca = "/data/a.tif"
                network.outNet = "/data/net.tif"
                network.process()
                """);
        assertEquals(result.errors().toString(), 2, result.errors().size());
        assertTrue(result.errors().get(0), result.errors().get(0).contains("inFlow of Tca reads"));
        assertTrue(result.errors().get(0), result.errors().get(0).contains("gets pitfiller.outPit, which is Pit."));
        assertTrue(result.errors().get(1), result.errors().get(1).contains("inTca of ExtractNetwork reads"));
    }
}
