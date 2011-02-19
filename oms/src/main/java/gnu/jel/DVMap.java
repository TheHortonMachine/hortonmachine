/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 * $Id: DVMap.java 490 2006-10-01 16:08:04Z metlov $
 *
 * This file is part of the Java Expressions Library (JEL).
 *   For more information about JEL visit :
 *    http://kinetic.ac.donetsk.ua/JEL/
 *
 * (c) 1998 -- 2007 by Konstantin Metlov(metlov@kinetic.ac.donetsk.ua);
 *
 * JEL is Distributed under the terms of GNU General Public License.
 *    This code comes with ABSOLUTELY NO WARRANTY.
 *  For license details see COPYING file in this directory.
 */

package gnu.jel;

/**
 * Provides the information about defined dynamic variables.
 * <P> Allows to translate variable names into
 * constants of Java primitive types at compile-time. See the section of the
 * manual on dynamic variables.
 */
public abstract class DVMap {

  /**
   * Returns the name of the type of the named property.
   * <P>The dot ('.') symbol can be present in the property name to denote
   * hierarchical naming scheme.
   * <P>If hierarchical naming scheme is used and the variable
   * x.y is defined the variable x must also be defined.
   * @param name is the name of the property.
   * @return the XXX in the name of the corresponding getXXXProperty()
   * method of dynamic library, null if the variable with the given name
   * is not defined.
   */
  public abstract String getTypeName(String name);

  /**
   * Translates the variable name (a String) to a constant of any primtive
   * type (e.g. a number).
   *
   * <P> The performance of the compiled code can be sometimes improved
   * by letting it not to deal with strings. For example, in older JEL <=0.9.8
   * this method was absent, and, if the underlying representation of the
   * dynamic variables storage was an array, the translation of variable name
   * (represented by String) into integer index had to happen at run-time,
   * taking up the valuable processor cycles. Defining the proper 
   * DVMap.translate one can perform the translation 
   * "variable name(String)"->"slot number(int)" at compile
   * time. There can be also other clever ways of using translation to
   * improve performance even if variables are not stored in an array or
   * vector.
   * <P> The default implementation provides the identity translation, which
   * simulates the behaviour of older versions of JEL.
   *
   * @since 0.9.9
   * @param name Name of the variable to be translated.
   * @return Object representing the translated name, this object must be
   * either a reflection type wrapping a primitive (e.g. java.lang.Integer,
   * java.lang.Byte) or String. No other object is allowed, othwerwise an
   * exception will emerge at compile-time. This limitation is due to Java 
   * class file format not allowing to store constants of types other than
   * specified above.
   */
  public Object translate(String name) {
    return name;
  };

};
