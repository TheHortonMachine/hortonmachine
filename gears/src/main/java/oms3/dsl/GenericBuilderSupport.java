/*
 * $Id: GenericBuilderSupport.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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

import groovy.util.BuilderSupport;
import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import oms3.ComponentException;

/**
 * Generic Builder class. Simplifies the use of Groovy's BuilderSupport API.
 *
 * @author od
 */
public abstract class GenericBuilderSupport extends BuilderSupport {

    private static final Logger log = Logger.getLogger("oms3.sim");
//    private static final Logger model_log = Logger.getLogger("oms3.model");
    //
    static final ConsoleHandler conHandler = new ConsoleHandler();
    //
    Buildable current;

    static public class LR extends Formatter {

        @Override
        public String format(LogRecord r) {
            return String.format("%1$tm/%1$td %1$tT %2$-7s %3$s\n",
                    new Date(r.getMillis()), r.getLevel(), r.getMessage());
        }
    }

    static public class CompLR extends Formatter {

        @Override
        public String format(LogRecord r) {
            return String.format("%1$tm/%1$td %1$tT %2$-7s [%3$s] %4$s\n",
                    new Date(r.getMillis()), r.getLevel(), r.getLoggerName(), r.getMessage());
        }
    }

    static {
        log.setUseParentHandlers(false);
        log.addHandler(conHandler);
        conHandler.setLevel(Level.ALL);   // otherwise it blocks on CONFIG and below.
        conHandler.setFormatter(new LR());
//        log.setLevel(Level.parse(System.getProperty("loglevel")));
    }

    public GenericBuilderSupport() {
        if (log.isLoggable(Level.CONFIG)) {
            SortedMap sysProp = new TreeMap(System.getProperties());
            log.config("System properties -------");
            for (Object key : sysProp.keySet()) {
                log.config(key + ": " + sysProp.get(key));
            }
            log.config("End system properties -------");
        }
    }

//    public void setLogging(String level) {
//        System.out.println("setLogging  " + level);
//        log.setLevel(Level.parse(level));
//    }
    @Override
    protected Object createNode(Object name) {
        return createNode(name, null, null);
    }

    @Override
    protected Object createNode(Object name, Object value) {
        return createNode(name, null, value);
    }

    @Override
    protected Object createNode(Object name, Map map) {
        return createNode(name, map, null);
    }

    @Override
    protected void setParent(Object parent, Object child) {
    }

    /**
     * Provide the entry classes for a builder.
     *
     * @param name the src object
     * @return the builder object.
     */
    protected abstract Class<? extends Buildable> lookupTopLevel(Object name);

    /**
     * Create the nodes.
     *
     * @param name the name
     * @param props the map of properties
     * @param value the value
     * @return the Object the configure.
     */
    @Override
    protected Object createNode(Object name, Map props, Object value) {
        if (log.isLoggable(Level.CONFIG)) {
            log.config("name=" + name + ", " + "map=" + props + ", " + "value=" + value + ", " + "value type="
                    + (value != null ? value.getClass().toString() : "-"));
        }
        if (name == null) {
            throw new Error("name == null");
        }
        if (current == null) {
            Class<? extends Buildable> current_class = lookupTopLevel(name);
            try {
                current = current_class.newInstance();
            } catch (Exception ex) {
                throw new Error(ex.getMessage());
            }
        } else {
            current = current.create(name, value);
        }
        if (current == null) {
            throw new Error("current==null");
        }
        // Set properties if provided.
        if (props != null && current != Buildable.LEAF) {
            try {
                BeanBuilder b = new BeanBuilder(current.getClass());
                b.setProperties(current, props);
            } catch (IllegalAccessException ex) {
                throw new ComponentException(ex.getMessage());
            } catch (InvocationTargetException ex) {
                throw new ComponentException(ex.getTargetException().getMessage());
            } catch (IntrospectionException ex) {
                throw new ComponentException(ex.getMessage());
            }
        }
        return current;
    }

    @Override
    protected void nodeCompleted(Object parent, Object node) {
        current = (Buildable) parent;
        if (log.isLoggable(Level.CONFIG)) {
            log.config("Completed: " + parent + " " + node);
        }
    }
}
