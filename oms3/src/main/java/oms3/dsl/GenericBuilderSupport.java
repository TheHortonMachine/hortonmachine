/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl;

import groovy.util.BuilderSupport;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import ngmf.util.ConsoleLoggingHandler;

/**
 * Generic Builder class. Simplifies the use of Groovy's
 * BuilderSupport API.
 * 
 * @author od
 */
public abstract class GenericBuilderSupport extends BuilderSupport {

    Buildable current;
    private static final Logger log = Logger.getLogger("oms3.sim");
    private static final Logger model_log = Logger.getLogger("oms3.model");
    //
    static final ConsoleHandler conHandler = new ConsoleLoggingHandler();

    static {
        log.addHandler(conHandler);
        model_log.addHandler(conHandler);
        conHandler.setLevel(Level.ALL);
    }

    public GenericBuilderSupport() {
         if (log.isLoggable(Level.CONFIG)) {
            log.config("oms.version : " + System.getProperty("oms.version"));
            log.config("oms.home : " + System.getProperty("oms.home"));
            log.config("oms.prj : " + System.getProperty("oms.prj"));
        }
    }

    public void setLogging(String level) {
        log.setLevel(Level.parse(level));
    }

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
     * @param name
     * @return the  builder object.
     */
    protected abstract Class<? extends Buildable> lookupTopLevel(Object name);

    /** Handle the exception
     *
     * @param ex
     * @throws RuntimeException
     */
    private void handleException(Throwable ex) throws RuntimeException {
        ex.printStackTrace(System.out);     // there is no stack trace within NB.
        throw new RuntimeException(ex);
    }

    /**
     * Create the nodes.
     * 
     * @param name
     * @param props
     * @param value
     * @return the Object the configure.
     */
    @Override
    protected Object createNode(Object name, Map props, Object value) {
        if (log.isLoggable(Level.CONFIG)) {
            log.config("name=" + name + ", " + "map=" + props + ", " + "value=" + value +  ", " + "value type="
                    + (value != null ? value.getClass().toString() : "-"));
        }
        try {
            if (name == null) {
                throw new NullPointerException("name");
            }
            if (current == null) {
                current = lookupTopLevel(name).newInstance();
            } else {
                current = current.create(name, value);
            }
            if (current == null) {
                throw new NullPointerException("current");
            }
            // Set properties if provided.
            if (props != null && current != Buildable.LEAF) {
                BeanBuilder b = new BeanBuilder(current.getClass());
                b.setProperties(current, props);
            }
        } catch (Exception ex) {
            handleException(ex);
        }
        return current;
    }

    @Override
    protected void nodeCompleted(Object parent, Object node) {
        current = (Buildable) parent;
        if (log.isLoggable(Level.CONFIG)) {
            log.log(Level.CONFIG, "Completed: {0} - {1}", new Object[]{parent, node});
        }
    }
}
