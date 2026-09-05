/*
 * $Id: Notification.java 2fb4d513ea17 2014-06-04 odavid@colostate.edu $
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

import java.util.EventListener;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.EventListenerList;

/** Event Notification class. This class handles
 *  Allows the 
 *
 * @author od  
 *   
 */
public class Notification {

    EventListenerList ll = new EventListenerList();
    
    Controller c;
    /* avoid Event object creation and fire(..) calls if no listeners. */
    boolean shouldFire = false;
    
//    private static Notification instance;
//    
//    public static final Notification instance() {
//        if (instance == null) {
//            instance = new Notification();
//        }
//        return instance;
//    }
    
    protected static final Logger log = Logger.getLogger("oms3.sim");

    Notification(Controller c) {
        this.c = c;
    }

    final boolean shouldFire() {
        return shouldFire;
    }

    Controller getController() {
        return c;
    }

    void addListener(Listener l) {
        if (log.isLoggable(Level.CONFIG)) {
            log.config("Adding Notification Listener " + l);
        }
        ll.add(Listener.class, l);
        shouldFire = true;
    }

    void removeListener(Listener l) {
        if (log.isLoggable(Level.CONFIG)) {
            log.config("Removing Notification Listener " + l);
        }
        ll.remove(Listener.class, l);
        shouldFire = ll.getListenerCount() > 0;
    }

    private void fire(Type t, EventObject E) {
        Object[] listeners = ll.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            ((Listener) listeners[i + 1]).notice(t, E);
        }
    }

    void fireWait(ComponentAccess w) {
        if (shouldFire) {
            fire(Type.WAITING, new ComponentEvent(c, w.getComponent()));
        }
    }

    void fireStart(ComponentAccess w) {
        if (shouldFire) {
            fire(Type.EXECUTING, new ComponentEvent(c, w.getComponent()));
        }
    }

    void fireFinnish(ComponentAccess w) {
        if (shouldFire) {
            fire(Type.FINISHED, new ComponentEvent(c, w.getComponent()));
        }
    }

    void fireException(ComponentException E) {
        if (shouldFire) {
            fire(Type.EXCEPTION, new ExceptionEvent(c, E));
        }
    }

//    void fireConnect(Access srcAccess, Access destAccess) {
//        if (shouldFire) {
//            fire(Type.CONNECT, new ConnectEvent(c, srcAccess, destAccess));
//        }
//    }

//    void fireMapIn(Access srcAccess, Access destAccess) {
//        if (shouldFire) {
//            fire(Type.MAPIN, new ConnectEvent(c, srcAccess, destAccess));
//        }
//    }

//    void fireMapOut(Access srcAccess, Access destAccess) {
//        if (shouldFire) {
//            fire(Type.MAPOUT, new ConnectEvent(c, srcAccess, destAccess));
//        }
//    }

    void fireIn(DataflowEvent e) {
        fire(Type.IN, e);
    }

    void fireOut(DataflowEvent e) {
        fire(Type.OUT, e);
    }

    /** 
     * Notification Types. The values classify an event object.
     */
    public enum Type {

        /**
         * Execution waiting here for all @In to arrive
         * @see ComponentEvent
         */
        WAITING,
        /** A component is about to be executed. 
         * @see ComponentEvent
         */
        EXECUTING,
        /** 
         * A component is done with execution
         * @see ComponentEvent
         */
        FINISHED,
        /** 
         * A components @In field is receiving a value
         * @see ComponentEvent
         */
        IN,
        /** 
         * A components @Out field is providing a value
         * @see ConnectEvent
         */
        OUT,
        /** 
         * Exception was thrown by a component
         * @see ConnectEvent
         */
        EXCEPTION,
        /**
         * An @In field is connected to an out field of a component.
         * @see DataflowEvent
         */
//        CONNECT,
//        /** 
//         * A Component field is mapped to an @In field of
//         * a containing component.
//         * @see DataflowEvent
//         */
//        MAPIN,
//        /** 
//         * A Component field is mapped to an @Out field of
//         * a containing component.
//         * 
//         * @see DataflowEvent
//         */
//        MAPOUT,
//        FIELDIN,
//        FIELDOUT,
//        VALIN
        
    }

    /**
     * Notification Listener.
     */
    public interface Listener extends EventListener {

        /**
         * Called when an event happens.
         * @param t event type
         * @param E the event
         */
        // stateChanged(Type t, EventObject E);
        void notice(Type t, EventObject E);
    }

    /**
     * Connection Event.
     */
    public static class ConnectEvent extends EventObject {

        private static final long serialVersionUID = 1410979580285808419L;
        Access from;
        Access to;

        ConnectEvent(Object src, Access from, Access to) {
            super(src);
            this.from = from;
            this.to = to;
        }

        /**
         * Get the source of the connect event
         * @return the field access object being the connect source
         */
        public Access getFrom() {
            return from;
        }

        /**
         * Get the destination for the connect event.
         *
         * @return the target Field access component.
         */
        public Access getTo() {
            return to;
        }

        @Override
        public String toString() {
            return "Connect: " + from + " -> " + to;
        }
    }

    /**
     * Component Event.
     */
    public static class ComponentEvent extends EventObject {

        private static final long serialVersionUID = -8569599337868335893L;
        Object comp;

        ComponentEvent(Object src, Object comp) {
            super(src);
            this.comp = comp;
        }

        /** Get the component for this event. 
         *
         * @return the component 
         */
        public Object getComponent() {
            return comp;
        }

        @Override
        public String toString() {
            return "Component: " + getComponent();
        }
    }

    /**
     * Exception Event.
     * An exception occurred during component execution.
     * 
     */
    public static class ExceptionEvent extends EventObject {

        private static final long serialVersionUID = -1136021018405823527L;
        ComponentException E;

        ExceptionEvent(Object src, ComponentException E) {
            super(src);
            this.E = E;
        }

        /**
         * Get the Component exception.
         * @return the exception
         */
        public ComponentException getException() {
            return E;
        }

        @Override
        public String toString() {
            if (E == null) {
                return "Exception: NULL";
            }
            StringBuilder b = new StringBuilder("\n");
            if (E.getCause() != null) {
                for (StackTraceElement ste : E.getCause().getStackTrace()) {
                    b.append("  ").append(ste.toString()).append("\n");
                }
            }
            return "Exception: " + E.getMessage() + " in '" + E.getSource() + "'" + b.toString();
        }
    }

    /** 
     * Data flow event.
     * 
     */
    public static class DataflowEvent extends EventObject {

        private static final long serialVersionUID = -5551146005283344251L;
        Access data;
        Object value;

        DataflowEvent(Object source, Access data, Object value) {
            super(source);
            this.data = data;
            this.value = value;
        }

        /**
         * Get field access info.
         * Note: if you need to alter the value, do not use the Access object
         * method setFieldValue(). call setValue() on this object instead.
         * @return The field access object
         */
        public Access getAccess() {
            return data;
        }

        /**
         * Get the data value that is passed on @In/@Out
         * @return the data value that is passed around.
         */
        public Object getValue() {
            return value;
        }

        /** This methods allows altering the value being passed from @Out to @In.
         * Call this from within the 'notice' event notification when
         *  you receive this DataflowEvent. An example would be a unit conversion.
         *
         * @param value the altered value.
         */
        public void setValue(Object value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Flow: " + data.getComponent() + "@" + data.getField().getName() + " [" + value + "]";
        }
    }
}
