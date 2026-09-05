/*
 * $Id: Time.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
package oms3.control;

import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import java.util.GregorianCalendar;

/** 
 * Time Control Component
 *  
 * @author od
 *   
 */
public class Time extends While {
    
    @In  public GregorianCalendar start;
    @In  public GregorianCalendar end;
    @In  public int field;
    @In  public int amount;
    
    public static class TimeControl  {

        @Out public boolean done;
        @Out public GregorianCalendar current;
        
        @In  public GregorianCalendar start;
        @In  public GregorianCalendar end;
        @In  public int field;
        @In  public int amount;

        TimeControl() {
            current = start;
        }
        
        public void initialize() {
            current = start;
        }

        @Execute 
        public void execute() {
            if (current == null) {
                current = start;
            }
            current.add(field, amount);
            done = current.before(end);
        }
    }

    TimeControl tc = new TimeControl();

    public Time() {
        conditional(tc, "done");
            
        in2in("start", tc, "start");
        in2in("end", tc, "end");
        in2in("field", tc, "field");
        in2in("amount", tc, "amount");
    }
        
    protected void connectTime(Object cmd, String curr) {
        out2in(tc, "current", cmd, curr);
    }
}
