/*
 * $Id: Params.java 9cf2884d49f6 2013-06-18 odavid@colostate.edu $
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
package oms3.dsl;

import java.util.ArrayList;
import java.util.List;

public class Params implements Buildable {

    public static final String DSL_NAME = "parameter";
    // parameter file
    List<Param> param = new ArrayList<Param>();
    String file;

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
