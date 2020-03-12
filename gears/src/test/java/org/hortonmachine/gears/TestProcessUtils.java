package org.hortonmachine.gears;

import java.util.Arrays;

import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.processes.CommandExecutor;
/**
 * Test process utils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestProcessUtils extends HMTestCase {

    public void testBackSlashes() throws Exception {

        String command = "java -splash:$DIR/imgs/splash_dbviewer.png $MEM -Djava.util.logging.config.file=$DIR/quiet-logging.properties -Djava.library.path=$DIR/natives/ -cp \"$DIR/libs/*\" \"$DIR/folder with spaces/\" org.hortonmachine.database.DatabaseViewer $1";
        String[] expected = { //
                "java", //
                "-splash:$DIR/imgs/splash_dbviewer.png", //
                "$MEM", //
                "-Djava.util.logging.config.file=$DIR/quiet-logging.properties", //
                "-Djava.library.path=$DIR/natives/", //
                "-cp", //
                "\"$DIR/libs/*\"", //
                "\"$DIR/folder with spaces/\"", //
                "org.hortonmachine.database.DatabaseViewer", //
                "$1"//
        };

        CommandExecutor cexec = new CommandExecutor(command);
        String[] arguments = cexec.getArguments();
        assertTrue(Arrays.equals(expected, arguments));
    }

}
