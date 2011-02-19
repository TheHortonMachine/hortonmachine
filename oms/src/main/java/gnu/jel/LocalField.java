/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: LocalField.java 490 2006-10-01 16:08:04Z metlov $
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

import gnu.jel.debug.Debug;
import java.lang.reflect.Member;

/**
 * Represents a field local to the class being compiled.
 */
public class LocalField implements Member {
  private int modifiers;
  private java.lang.String name;
  private Class type;
  private Object constValue;

  /**
   * Constructs a new local field.
   * @param modifiers field modifiers, a sum of one or more of <TT>PUBLIC</TT>,
   *                <TT>PRIVATE</TT>,<TT>PROTECTED</TT>, <TT>STATIC</TT>,
   *                <TT>FINAL</TT>,<TT>VOLATILE</TT>, <TT>TRANSIENT</TT> 
   *                constants defined in java.lang.reflect.Modifier
   * @param type is a class representing the type of this field.
   * @param name is the name of this field.
   * @param constValue is the value of this field if it is static final,
   *                   <TT>null</TT> otherwise.
   */
  public LocalField(int modifiers, Class type, java.lang.String name, Object constValue){
    if (Debug.enabled)
      Debug.check((constValue==null) || ((modifiers & 0x0018) ==0x0018));

	this.type=type;
	this.name=name;
	this.modifiers=modifiers;
    this.constValue=constValue;
  };

  public Class getDeclaringClass() {
  	return null; // means local field
  };

  public java.lang.String getName() {
	return name;
  };

  public int getModifiers() {
	return modifiers;
  };
    
  public Class getType() {
	return type;
  };

  public boolean isSynthetic() {
    return true;
  };

  /**
   * Returns a value of the public static final field.
   * <P>Fails assertion if called on the field which is not public
   *  static final.
   * @return value of the field, object of wrapped primitive type or string.
   */
  public Object getConstValue() {
    if (Debug.enabled)
      Debug.check(constValue!=null);
    return constValue;
  };

};
