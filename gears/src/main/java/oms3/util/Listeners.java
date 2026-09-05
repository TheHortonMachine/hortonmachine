/*
 * $Id: Listeners.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.Notification.*;

import oms3.annotations.Range;
import oms3.annotations.Unit;

/**
 * Listener support classes.
 *  
 * @author od 
 *   
 */
public class Listeners {

    /**  
     * Range checker.
     * 
     * A range checker that checks for In/Out values tha have an additional 
     * "@Range" annotation.
     */
    public static class RangeCheck implements Listener {

        @Override
        public void notice(Type T, EventObject E) {
            if (T == Type.IN || T == Type.OUT) {
                DataflowEvent ce = (DataflowEvent) E;
                if (ce.getValue() instanceof Number || ce.getValue() != null) {
                    Range range = ce.getAccess().getField().getAnnotation(Range.class);
                    if (range != null) {
                        Number v = (Number) ce.getValue();
                        if (v != null) {
                            if (range.min() > v.doubleValue() || range.max() < v.doubleValue()) {
                                System.out.println(v + " not within expected range " +
                                        range.min() + "..." + range.max() + " in " + ce.getAccess().getComponent() + ")");
                            }
                        } else {
                            System.out.println("Null value");
                        }
                    }
                }
            }
        }
    }

    /**
     * Null checker.
     * Check for 'null' being passed around as @In or @Out values.
     */
    public static class NullCheck implements Listener {

        @Override
        public void notice(Type T, EventObject E) {
            if (T == Type.IN || T == Type.OUT) {
                DataflowEvent ce = (DataflowEvent) E;
                if (ce.getValue() == null) {
                    System.out.println("Null : " + ce.getAccess().getField().getName() + " in " + ce.getAccess().getComponent());
                }
            }
        }
    }

    /** 
     * Unit converter.
     */
    public static class UnitConverter implements Listener {

        // mapping data -> output unit 
        Map<Object, String> m = new HashMap<Object, String>();

        @Override
        public void notice(Type T, EventObject E) {
            if (T == Type.OUT) {
                DataflowEvent ce = (DataflowEvent) E;
                Unit outUnit = ce.getAccess().getField().getAnnotation(Unit.class);
                if (outUnit != null) {
                    m.put(ce.getValue(), outUnit.value());
                }
            } else if (T == Type.IN) {
                DataflowEvent ce = (DataflowEvent) E;
                String out = m.get(ce.getValue());
                if (out != null) {
                    Unit inUnit = ce.getAccess().getField().getAnnotation(Unit.class);
                    if (inUnit != null) {
                        convert(out, inUnit.value(), ce.getValue());
                    }
                }
            }
        }

        private void convert(String out, String in, Object value) {
        }
    }

    public static class Printer implements Listener {

        @Override
        public void notice(Type T, EventObject E) {
            System.out.printf(" '%s' from %s\n", T, E.getSource());
            if (T == Type.EXCEPTION) {
                ExceptionEvent ce = (ExceptionEvent) E;
                System.out.printf(">>>>>>'%s' in %s\n", ce.getException());
            }
            if (T == Type.OUT) {
                DataflowEvent ce = (DataflowEvent) E;
                System.out.printf("     '%s' -> \n", ce.getValue());
            }
            if (T == Type.IN) {
                DataflowEvent ce = (DataflowEvent) E;
                System.out.printf("        -> '%s'\n", ce.getValue());
            }
//                if (E.getEventType() == ExecutionEvent.Type.START && E.getCommand() == c.op1) {
//                    throw new InterruptedException("stop it.");
//                }
        }
    }

    /** 
     *  Simple Logging listener.
     */
    public static class Logging implements Listener {

        Logger log;
        Level level;

        public Logging(Logger log, Level level) {
            this.log = log;
            this.level = level;
        }

        @Override
        public void notice(Type T, EventObject E) {
            log.log(level, T + ": " + E);
        }
    }
}
