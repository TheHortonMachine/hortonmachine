/*
 * $Id: Utils.java 1fe99f9c399e 2018-12-16 francesco.serafin.3@gmail.com $
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
package oms3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author od
 */
public class Utils {

    static String oms_version = null;

    // this string is being managed from the build file with keyword substitution.
    static final String ver = "#version: 3.5.9 5aa6bb98e6ee 2018-12-11 #";


    static synchronized String getMajorVersion() {
        String[] v = ver.split("\\s+");
        if (v.length >= 2) {
            String[] tag = v[1].split("\\.+");
            if (tag.length == 3) {
                return tag[0] + "." + tag[1];
            }
        }
        return "0.0";
    }


    static synchronized String getVersion() {
        String[] v = ver.split("\\s+");
        if (v.length >= 2) {
            return v[1];
        }
        return "0.0";
    }


    public static void main(String[] args) {
        System.out.println(getVersion());
    }


    /**
     * Returns a map of the fields with the field name as the key.
     *
     * @param fields the fields
     * @return the field map
     */
    static Map<String, Field> getFieldMap(Field[] fields) {
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : fields) {
            fieldMap.put(field.getName(), field);
        }
        return fieldMap;
    }


    static MethodInvoker methodInvoker(final Object target, final Method method) {
        return reflect(target, method);
    }


    static FieldInvoker fieldInvoker(final Object target, final String field) throws NoSuchFieldException {
        return reflect(target, field);
    }


    static FieldInvoker fieldInvoker(final Object target, final Field field) {
        return reflect(target, field);
    }


    /**
     * Reflective invocation
     *
     * @param target
     * @param method
     * @return
     */
    private static MethodInvoker reflect(final Object target, final Method method) {
        return new MethodInvoker() {
            @Override
            public void invoke() throws Exception {
                method.invoke(target);
            }
        };
    }


    private static FieldInvoker reflect(final Object target, final String field) throws NoSuchFieldException {
        return reflect(target, target.getClass().getField(field));
    }


    private static FieldInvoker reflect(final Object target, final Field field) {
        field.setAccessible(true);
        return new FieldInvoker() {
            @Override
            public Object getValue() throws Exception {
                return field.get(target);
            }


            @Override
            public void setValue(Object value) throws Exception {
                field.set(target, value);
            }
        };
    }


}
