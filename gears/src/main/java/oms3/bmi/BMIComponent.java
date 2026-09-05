/*
 * $Id: BMIComponent.java dba0d06e37a0 2015-04-14 odavid@colostate.edu $
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
package oms3.bmi;

import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;
import oms3.Access;
import oms3.ComponentAccess;
import oms3.Conversions;
import oms3.annotations.*;

/**
 * BMI wrapper for an OMS component.
 *
 * @author od, archita
 */
public class BMIComponent implements BMI {

    ComponentAccess comp;


    BMIComponent(Object omscomp) {
        comp = new ComponentAccess(omscomp);
    }


    public static BMI create(Object comp) {
        return new BMIComponent(comp);
    }


    @Override
    public void initialize(String config_file) throws Exception {
        Reader r = new FileReader(config_file);
        Properties p = new Properties();
        p.load(r);
        for (String name : p.stringPropertyNames()) {
            Access a = comp.input(name);
            Object o = Conversions.convert(p.getProperty(name), a.getField().getType());
            a.setFieldValue(o);
        }
        r.close();
        ComponentAccess.callAnnotated(comp.getComponent(), Initialize.class, true);
    }


    @Override
    public void update() {
        ComponentAccess.callAnnotated(comp.getComponent(), Execute.class, false);
    }


    @Override
    public void finalize() {
        ComponentAccess.callAnnotated(comp.getComponent(), Finalize.class, true);
    }


    @Override
    public String getComponentName() {
        Name n = comp.getComponent().getClass().getAnnotation(Name.class);
        return n != null ? n.value() : "<none>";
    }


    @Override
    public int getInputVarNameCount() {
        return comp.inputs().size();
    }


    @Override
    public int getOutputVarNameCount() {
        return comp.outputs().size();
    }


    @Override
    public String[] getInputVarNames() {
        Access[] a = comp.inputs().toArray(new Access[0]);
        return toString(a);
    }


    @Override
    public String[] getOutputVarNames() {
        Access[] a = comp.outputs().toArray(new Access[0]);
        return toString(a);
    }


    @Override
    public Object getValue(String name) throws Exception {
        Access a = comp.output(name);
        if (a == null) {
            throw new IllegalArgumentException("No such name " + name);
        }
        return a.getField().get(comp.getComponent());
    }


    @Override
    public void setValue(String name, Object value) throws Exception {
        Access a = comp.output(name);
        if (a == null) {
            throw new IllegalArgumentException("No such name " + name);
        }
        a.getField().set(comp.getComponent(), value);
    }


    private String[] toString(Access[] a) {
        String[] s = new String[a.length];
        for (int i = 0; i < a.length; i++) {
            s[i] = a[i].getField().getName();
        }
        return s;
    }


    @Override
    public String getVarType(String name) {
        Access a = comp.output(name);
        if (a == null) {
            throw new IllegalArgumentException("No such name " + name);
        }
        return a.getField().getType().toString();
    }


    @Override
    public long getVarItemsize(String name) {
        // this is more involved here ... 
        return 0;
    }


    @Override
    public String getVarUnits(String name) {
        Access a = comp.output(name);
        if (a == null) {
            throw new IllegalArgumentException("No such name " + name);
        }
        Unit u = a.getField().getAnnotation(Unit.class);
        return u != null ? u.value() : "";
    }


    @Override
    public void getCurrentTime(double time) {
    }


    @Override
    public void getStartTime(double time) {
    }


    @Override
    public void getEndTime(double end) {
    }


    @Override
    public void getTimeStep(double dt) {
    }


    @Override
    public void getTimeUnits(String units) {
    }


    @Override
    public String getGridType() {
        return "scalar";
    }
}
