/*
 * $Id: Grid.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
package oms3.ds;

import oms3.Compound;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Grid datastaructure.
 * 
 * @author od  
 *   
 */
public class Grid extends ArrayList<Compound> {
    
    private static final long serialVersionUID = -1159467302002888533L;
    

    int cols;
    int rows;
    
    public Grid(int width, int height) {
        super(width * height);
        this.cols = width;
        this.rows = height;
    }

    private int index(int x, int y) {
        return x * cols + y;
    }

    public void set(int x, int y, Compound w) {
        super.set(index(x, y), w);
    }

    public Compound get(int x, int y) {
        return super.get(index(x, y));
    }

    public Iterator<Compound> col_wise() {
        
        return new Iterator<Compound>() {

            int y = 0;
            int x = 0;
            int idx = 0;
            
            public boolean hasNext() {
                if ((x == cols-1) && (y < rows-1)) {
                    x = 0;
                }
                return true;
            }

            public Compound next() {
                idx++;
                return get(x,y);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    public Iterator<Compound> row_wise() {
        return super.iterator();
    }
}

