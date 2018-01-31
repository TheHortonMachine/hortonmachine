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
package oms3.ds;

import oms3.Compound;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Grid datastaructure.
 * 
 * @author Olaf David (olaf.david@ars.usda.gov)
 * @version $Id$ 
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

