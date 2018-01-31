package oms3.dsl.analysis;

import java.io.File;
import oms3.dsl.*;
import oms3.ngmf.util.OutputStragegy;

/**
 * 
 * @author od
 */
public class EspTrace implements Buildable {

    String title = "ESP Traces";
    String dir;
    String var;
    String report;

    public void setReport(String report) {
        this.report = report;
    }

    public String getReport(OutputStragegy st) {
        if (report == null) {
            return null;
        }
        File f = new File(report);
        if (report.startsWith("%")) {
            f = OutputStragegy.resolve(new File(st.baseFolder(), report));
        } else {
            f = OutputStragegy.resolve(report);
        }
        return f.toString();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public String getVar() {
        return var;
    }

    public String getDir(OutputStragegy st) {
        File f = new File(dir);
        if (!(f.isAbsolute() && f.exists())) {
            if (dir.startsWith("%")) {
                f = OutputStragegy.resolve(new File(st.baseFolder(), dir));
            } else {
                f = OutputStragegy.resolve(dir);
            }
        }
        return f.toString();
    }

    @Override
    public Buildable create(Object name, Object value) {
        return LEAF;
    }
}
