/*
 * $Id: ProcessComponent.java e398ba21b4c6 2014-03-07 odavid@colostate.edu $
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

import java.io.File;
import java.io.StringWriter;
import java.util.logging.Logger;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;


/** Generic Process component.
 *
 * @author od
 */
public class ProcessComponent {
    static final Logger log = Logger.getLogger(ProcessComponent.class.getName());

    @In public String exe;
    @In public String[] args = new String[]{};
    @In public String working_dir;

    @Out public String stdout;
    @Out public String stderr;
    
    @Out public int exitValue;

    @Execute
    public void execute() {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        try {
            ProcessExecution p = new ProcessExecution(new File(exe));
            p.setArguments((Object[]) args);

            if (working_dir != null && !working_dir.isEmpty()) {
                p.setWorkingDirectory(new File(working_dir));
            }
            p.redirectOutput(out);
            p.redirectError(err);

            exitValue = p.exec();
            stdout = out.toString();
            stderr = err.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
