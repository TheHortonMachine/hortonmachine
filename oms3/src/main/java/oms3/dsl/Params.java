package oms3.dsl;

import java.util.ArrayList;
import java.util.List;

public class Params implements Buildable {

    // parameter file
    String file;
    List<Param> param = new ArrayList<Param>();

    @Override
    public Buildable create(Object name, Object value) {
        Param p = new Param(name.toString(), value);
        param.add(p);
        return p;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFile() {
        return file;
    }

    public List<Param> getParam() {
        return param;
    }

    public int getCount() {
        return param.size();
    }
}
