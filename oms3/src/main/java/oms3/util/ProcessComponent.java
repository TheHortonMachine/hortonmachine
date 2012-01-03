/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import oms3.annotations.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/** Generic Process component.
 *
 * @author od
 */
public class ProcessComponent {
    static final Logger log = Logger.getLogger(ProcessComponent.class.getName());

    // the executable file
    @In public String exe;
    
    @In public String[] args;

    @In public String stdin;

    @In public String working_dir;
    @In public boolean verbose = false;

    @Out public String stdout;
    @Out public String stderr;
    @Out public int exitValue;

    @Execute
    public void execute() {
////        MyProcesses p = new MyProcesses(new File(exe));
////        p.setArguments((Object[]) args);
////
////        try {
////            if (stdin != null && !stdin.isEmpty()) {
////                p.redirectInput(new FileInputStream(stdin));
////            }
////            if (working_dir != null && !working_dir.isEmpty()) {
////                p.setWorkingDirectory(new File(working_dir));
////            }
////
////            final StringBuffer out_buff = new StringBuffer();
////            final StringBuffer err_buff = new StringBuffer();
////            p.redirectOutput(new OutputStream() {
////
////                @Override
////                public void write(int b) throws IOException {
////                    out_buff.append((char) b);
////                }
////            });
////
////            p.redirectError(new OutputStream() {
////
////                @Override
////                public void write(int b) throws IOException {
////                    err_buff.append((char) b);
////                }
////            });
////
////            exitValue = p.exec();
////            stdout = out_buff.toString();
////            stderr = err_buff.toString();
//            
//        } catch (Exception ex) {
//            throw new RuntimeException(ex);
//        }
    }
}
