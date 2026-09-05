/*
 * $Id: MmsDimension.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
package oms3.ngmf.ui.mms;

public class MmsDimension implements Dimension {
    private String name;
    private int size;
    private String[] item_names = null;
    private String[] item_desc = null;

    public MmsDimension(String n, int s) {
        name = n;
        size = s;
    }

    public String getName() {return (name);}
    public int getSize() {return (size);}

    public void setSize(int new_size) {
        if (item_names != null) {
            String[] new_item_names = new String[new_size];
            for (int i = 0; i < new_size; i++) {
                if (i < size) {
                    new_item_names[i] = item_names[i];
                } else {
                    new_item_names[i] = item_names[size - 1];
                }
            }
            item_names = null;
            item_names = new_item_names;
        }

        if (item_desc != null) {
            String[] new_item_desc = new String[new_size];
            for (int i = 0; i < new_size; i++) {
                if (i < size) {
                    new_item_desc[i] = item_desc[i];
                } else {
                    new_item_desc[i] = item_desc[size - 1];
                }
            }
            item_desc = null;
            item_desc = new_item_desc;
        }
        size = new_size;
    }

    public String[] getItemNames () {return item_names;}
    public String[] getItemDesc () {return item_desc;}
    public String toString () {return name;}

    public void addItemName(int i, String in, int size) {
        if (item_names == null) {
            item_names = new String[size];
        }
        item_names[i] = in;
    }

    public void addItemDesc(int i, String in, int size) {
        if (item_desc == null) {
            item_desc = new String[size];
        }
        item_desc[i] = in;
    }
}