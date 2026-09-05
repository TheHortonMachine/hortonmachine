/*
 * $Id: KVPContainer.java 107ba2bdf386 2014-12-26 odavid@colostate.edu $
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
package oms3.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author od
 */
public class KVPContainer implements Buildable {

    List<KVP> entries = new ArrayList<>();

    public List<KVP> getEntries() {
        return entries;
    }
    
    boolean hasEntries() {
        return entries.size()>0;
    }

    @Override
    public Buildable create(Object name, Object value) {
        entries.add(new KVP(name.toString(), value));
        return LEAF;
    }
}
