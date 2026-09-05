/*
 * $Id: Conditional.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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

import oms3.Compound;
import oms3.annotations.Execute;
import oms3.annotations.In;

/**
 *  Conditional Compound.
 * 
 * @author od  
 *   
 */
public abstract class Conditional extends Compound {

    public static class Cond {

        @In public boolean alive = true;

        @Execute
        public void execute() {
        }
    }
    
    protected Cond cond = new Cond();

    protected  void conditional(Object cmd, String bool) {
        out2in(cmd, bool, cond, "alive");
    }
}
