/*
 * $Id$
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
package oms3.dsl.cosu;

import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author od kmo
 */
public class RangeParser {

    public static List<Integer> parse(String range, int max) {
        List<Integer> idx = new ArrayList<Integer>();
        String[] n = range.split(",");
        for (String s : n) {
            String[] d = s.split("-");
            int mi = Integer.parseInt(d[0]);
            if (mi < 0 || mi >= max) {
                throw new IllegalArgumentException(range);
            }
            if (d.length == 2) {
                if (d[1].equals("*")) {
                    d[1] = Integer.toString(max - 1);
                }
                int ma = Integer.parseInt(d[1]);
                if (ma <= mi || ma >= max || ma < 0) {
                    throw new IllegalArgumentException(range);
                }
                for (int i = mi; i <= ma; i++) {
                    idx.add(i);
                }
            } else {
                idx.add(mi);
            }
        }
        return idx;
    }
    
    public static boolean[] getArray(String str, int length) {
        boolean[] mask = new boolean[length];
        List<Integer> idx = new ArrayList<Integer>();
        if (length > 1) {
            idx = parse(str, length);
        } else {
            // parseRange not happy with "0-*" for length of 1.
            idx.add(0);
        }
        for (int i = 0; i < length; i++) { // match all rows in the range provided
            mask[i] = idx.contains(i);
        }
        return mask;
    }
}
