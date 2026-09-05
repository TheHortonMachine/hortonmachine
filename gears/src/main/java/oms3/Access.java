/*
 * $Id: Access.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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
package oms3;

import java.lang.reflect.Field;

/** Access Interface for Fields.
 *
 * @author od
 */
public interface Access {

    /**
     * Reading (in) access.
     * 
     * @throws Exception generic exception
     */
    void in() throws Exception;

    /**
     * Writing (out) access
     * 
     * @throws Exception generic exception
     */
    void out() throws Exception;

    Field getField();

    Object getFieldValue() throws Exception;

    void setFieldValue(Object o) throws Exception;

    Object getComponent();

    /** Check if field access is valid.
     *
     * @return true if valid, false otherwise.
     */
    boolean isValid();

    FieldContent getData();

    void setData(FieldContent data);
    
}
