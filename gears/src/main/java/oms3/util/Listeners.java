/*
 * $Id$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
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
 * @author Olaf David 
 * @version $Id$ 
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
