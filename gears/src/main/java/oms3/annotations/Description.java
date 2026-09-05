/*
 * $Id: Description.java 50798ee5e25c 2013-01-09 odavid@colostate.edu $
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
package oms3.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Description information for fields and components.
 *
 * @see In
 * @author od 
 *   
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Description {

    /** The default value of a description
     *
     * @return the description string
     */
    String value() default "";

    // optional Localized descriptions below.
    String ar() default "";

    String be() default "";

    String bg() default "";

    String ca() default "";

    String cs() default "";

    String da() default "";

    String de() default "";

    String el() default "";

    String en() default "";

    String es() default "";

    String et() default "";

    String fi() default "";

    String fr() default "";

    String ga() default "";

    String hi() default "";

    String hr() default "";

    String hu() default "";

    String in() default "";

    String is() default "";

    String it() default "";

    String iw() default "";

    String ja() default "";

    String ko() default "";

    String lt() default "";

    String lv() default "";

    String mk() default "";

    String ms() default "";

    String mt() default "";

    String nl() default "";

    String no() default "";

    String pl() default "";

    String pt() default "";

    String ro() default "";

    String ru() default "";

    String sk() default "";

    String sl() default "";

    String sq() default "";

    String sr() default "";

    String sv() default "";

    String th() default "";

    String tr() default "";

    String uk() default "";

    String vi() default "";

    String zh() default "";
}
