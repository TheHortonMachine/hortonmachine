/*
 * $Id: CSProperties.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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
package oms3.io;

import java.util.Map;

/** Comma separated properties
 *
 * @author od
 */
public interface CSProperties extends Map<String, Object> {

    /** Get the name of the propertyset
     * 
     * @return the name
     */
    String getName();

    /**
     * Set the name
     * @param name the name
     */
    void setName(String name);

    /** Get the annotations for the propertyset.
     * 
     * @return the info for the propertyset.
     */
    Map<String, String> getInfo();

    /** Get the info for a property.
     * 
     * @param propertyName the name of the property
     * @return the annotations for this property.
     */
    Map<String, String> getInfo(String propertyName);

    void setInfo(String propertyname, Map<String, String> info);

    public void putAll(CSProperties p);
}
