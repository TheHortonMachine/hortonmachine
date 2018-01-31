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
package oms3.control;

import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import java.util.GregorianCalendar;

/** 
 * Time Control Component
 *  
 * @author od
 * @version $Id$ 
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
