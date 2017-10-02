/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/*******************************************************************************
           FILE:  AttributeTable.java
    DESCRIPTION:  
          NOTES:  ---
         AUTHOR:  John Preston,
                  Andrea Antonello
          EMAIL:  john.preston@uwimona.edu.jm
                  antonell@ing.unitn.it
        COMPANY:  University of the West Indies
                  University of Trento / CUDAM
      COPYRIGHT:  ICENS, University of the West Indies
                  Engineering Department, University of Trento / CUDAM
        VERSION:  
        CREATED:  August 11, 2004, 2004
       REVISION:  ---

 ******************************************************************************

   This library is free software; you can redistribute it and/or 
   modify it under the terms of the GNU Library General Public 
   License as published by the Free Software Foundation; either 
   version 2 of the License, or (at your option) any later version. 

   This library is distributed in the hope that it will be useful, 
   but WITHOUT ANY WARRANTY; without even the implied warranty of 
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
   Library General Public License for more details. 

   You should have received a copy of the GNU Library General Public 
   License along with this library; if not, write to the Free 
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 
   USA 

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions
   are met:
   1. Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
   2. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

 ******************************************************************************

     CHANGE LOG:

        version: 
       comments: changes
         author: 
        created:  
 *****************************************************************************/
package org.hortonmachine.gears.io.grasslegacy.map.attribute;

import java.util.Enumeration;
import java.util.Vector;

/**
 *
 */
public class AttributeTable {
    private Vector atts = null;

    /** Creates a new instance of AttributeTable */
    public AttributeTable() {
        atts = new Vector();
    }

    /**
     *
     */
    public int size() {
        return atts.size();
    }

    /**
     *
     */
    public Enumeration getCategories() {
        return atts.elements();
    }

    /**
     *
     */
    public void addAttribute( float cat, String value ) {
        // System.out.println("##############################################");
        // for (int i=0; i<atts.size(); i++)
        // System.out.println("BEFORE ATT["+i+"]="+atts.elementAt(i));
        // System.out.println("==================================");
        if (get(cat) == null) {
            insertAttribute(cat, value);
        }
        // System.out.println("==================================");
        // for (int i=0; i<atts.size(); i++)
        // System.out.println("AFTER ATT["+i+"]="+atts.elementAt(i));
        // System.out.println("##############################################");
    }

    public void addAttribute( float cat0, float cat1, String value ) {

    }

    /**
     *
     */
    public String get( float cat ) {
        int low = 0;
        int high = atts.size() - 1;

        while( low <= high ) {
            int i = (low + high) / 2;
            CellAttribute catt = (CellAttribute) atts.elementAt(i);
            int c = catt.compare(cat);
            if (c == 0) {
                return catt.getText();
            } else if (c < 0) {
                high = i - 1;
            } else {
                low = i++ + 1;
            }
        }
        return null;
    }

    private void insertAttribute( float cat, String value ) {
        int i = 0;
        int low = 0;
        int high = atts.size() - 1;

        while( low <= high ) {
            i = (low + high) / 2;
            CellAttribute catt = (CellAttribute) atts.elementAt(i);
            int c = catt.compare(cat);
            if (c == 0) {
                /* Attribute found with equal value so break
                 * and insert using this index. */
                low = high + 1;
            } else if (c < 0) {
                high = i - 1;
            } else {
                low = i++ + 1;
            }
        }
        atts.insertElementAt(new CellAttribute(cat, value), i);
    }

    /**
     *
     */
    public class CellAttribute {
        private float low = 0f;

        private float range = 0f;

        private String catText = null;

        /**
         *
         */
        public CellAttribute( float cat, String text ) {
            low = cat;
            range = 0;
            catText = text;
        }

        /**
         *
         */
        public CellAttribute( float cat0, float cat1, String text ) {
            if (cat1 > cat0) {
                low = cat0;
                range = cat1 - cat0;
            } else {
                low = cat1;
                range = cat0 - cat1;
            }
            catText = text;
        }

        /**
         * Compare a value to the range of values in this attribute
         * If the cat is below the renage then return -1, if it
         * is aboove the ramge then return +1, if it is equal return 0
         */
        public int compare( float cat ) {
            float diff = cat - low;
            if (diff < 0)
                return -1;
            else if (diff > range)
                return 1;

            return 0;
        }

        public String getText() {
            return catText;
        }

        public float getLowcategoryValue() {
            return low;
        }

        public float getCategoryRange() {
            return range;
        }

        /**
         *
         */
        public String toString() {
            if (range == 0f)
                return String.valueOf(low) + ":" + catText;
            else
                return String.valueOf(low) + "-" + String.valueOf(low + range) + ":" + catText;
        }
    }
}
