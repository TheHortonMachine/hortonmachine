/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: OP.java 490 2006-10-01 16:08:04Z metlov $
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
import java.util.Stack;

/**
 * A tree node, representing an operation.
 */
public abstract class OP {

  /** Holds references to children of this node */
  public OP[] chi=null;

  /** Holds type ID of the result of this OP */
  public int resID;
  
  /** Holds type of the result of this OP */
  public Class resType;

  /**
   * Called to evaluate this node and all its sub-nodes.
   * <P>Upon success this node is to be replaced by the constant node
   * holding the returned object.
   * @return an object to which this node evaluates
   * @exception if can't evaluate, in this case the sub-nodes
   */
  public abstract Object eval() throws Exception;

  /**
   * Called to generate the code implementing this OP.
   * @param cf class file with a new open method to write the code into.
   */
  public abstract void compile(ClassFile cf);

  //======================================================
  // Utility methods for dealing with types, etc...
  //======================================================

  /**
   *  Classes of the special types by ID.
   *  <P> The frequently used types (those on which many operations are
   *  defined) are identified by an integer number. The advantage is 
   *  the possibility to have almost completely table driven code generator.
   *  <P> So, the special types are only special in the fact that except
   *  of the reference to their class object they are also identified by an
   *  integer number.
   */
  public final static Class[] specialTypes; // defined in gnu.jel.TablesKeeper

  /**
   * Unwraps the type ID.
   * <P>That is all special types which are references are translated into 8.
   */
  public final static byte[] unwrapType;

  static {
    specialTypes=(Class[])TableKeeper.getTable("specialTypes");
    unwrapType=(byte[]) TableKeeper.getTable("unwrapType");
  };

  /**
   * Identifies the primitive type of the given class.
   * @param c class to identify.
   * @return id of the corresponding primitive type.
   */
  public static final int typeID(Class c) {

    final int NUM_SPECIAL_PRIMITIVE_TYPES=10;
    // ^^^ it is the number of primitive types out of special ones

    if (c==null) return 8;
    if (c.isPrimitive()) {
      int i;
      for(i=0;(i<NUM_SPECIAL_PRIMITIVE_TYPES) && (specialTypes[i]!=c);i++);
      if (Debug.enabled)
        Debug.check(i<NUM_SPECIAL_PRIMITIVE_TYPES,
                     "You didn't put _ALL_ primitive types"+
                     " into primitiveTypes array.");
      return i;
    };

    int i;
    for(i=NUM_SPECIAL_PRIMITIVE_TYPES+1; // TSB is excluded
        (i<specialTypes.length) && (!specialTypes[i].isAssignableFrom(c));
        i++);
    
    if (i<specialTypes.length) return i; 
    return 8; // just a generic reference
  };


  /**
   * Identify the primitive type corresponding to the given reflection object.
   * @param o object to identify.
   * @return id of the corresponding primitive type.
   */
  public static final int typeIDObject(Object o) {
    if (o instanceof java.lang.Boolean) return 0;
    if (o instanceof java.lang.Byte) return 1;
    if (o instanceof java.lang.Character) return 2;
    if (o instanceof java.lang.Short) return 3;
    if (o instanceof java.lang.Integer) return 4;
    if (o instanceof java.lang.Long) return 5;
    if (o instanceof java.lang.Float) return 6;
    if (o instanceof java.lang.Double) return 7;
    if (o instanceof java.lang.String) return 11;
    return 8;
  };

  // bitmap of allowed widening type conversions. 
  private final static int[] cvt_wide;
  static {
    cvt_wide=(int[])TableKeeper.getTable("cvt_wide");
  };

  /**
   * Used to find out if the conversion t1->t2 is widening.
   * @param id1 type ID to convert from
   * @param c1 class to convert from (used if id1==8)
   * @param id2 type ID to convert to
   * @param c2 class to convert to (used if id2==8)
   * @return true if the given conversion is widening (can be done
   *         automatically)
   */
  public static boolean isWidening(int id1, Class c1, int id2, Class c2) {
    if ((id2<=11) && (id2!=8)) {
      // converting into primitive
      id1=unwrapType[id1];
      if (Debug.enabled)
        Debug.check(id1<cvt_wide.length);
      
      return (cvt_wide[id2] & (0x800 >> id1)) >0;
    };

    // converting into object
    if (id1>=8) {
      // converting object into object
      if (c1==c2) return true; // for nulls also
      if (c1==null) return true; // "null" can be widened to any reference
      if (c2==null) return false; // assignment to "null" is narrowing
      return c2.isAssignableFrom(c1); // can assign references
    };

    // primitive into object is certainly not widening
    return false;
  };

  /**
   * Used to find out if the conversion t1->t2 is widening.
   * @param c1 class to convert from (used if id1==8)
   * @param c2 class to convert to (used if id2==8)
   * @return true if the given conversion is widening (can be done
   *         automatically)
   */
  public static boolean isWidening(Class c1, Class c2) {
    return isWidening(typeID(c1),c1,typeID(c2),c2);
  };

  /**
   * Makes widest possible representation of a value of Java primitive type.
   * @param o reflection object, containing value to represent.
   * @param clsID ID of a type of this reflection object (to save lookup).
   * @return Number, representing the given value.
   */
  protected static Number widen(Object o, int clsID) {
    switch (clsID) {
    case 0: // Z
      if (((Boolean)o).booleanValue()) 
        return new Long(1L); 
      else 
        return new Long(0L);
    case 1: // B
      return (Number)o;
    case 2: // C
      return new Long((long)((Character)o).charValue());
    case 3: // S
    case 4: // I
    case 5: // J
    case 6: // F
    case 7: // D
      return (Number)o;
    default:
      if (Debug.enabled)
        Debug.println("Attempt to widen wrong primitive ("+clsID+").");
      return new Long(0L);
    }
  };

  /**
   * Narrows the value back to desired primitiva type.
   * @param val reflection object, containing value to narrow.
   * @param clsID ID of a type to narrow the given object into.
   * @return narrowed reflection object.
   */
  protected static Object narrow(Number val, int clsID) {
    switch (clsID) {
    case 0: // Z
      if (val.longValue()!=0) return Boolean.TRUE; else return Boolean.FALSE;
    case 1: // B
      return new Byte(val.byteValue());
    case 2: // C
      return new Character((char)val.longValue());
    case 3: // S
      return new Short(val.shortValue());
    case 4: // I
      return new Integer(val.intValue());
    case 5: // J
      return new Long(val.longValue());
    case 6:
      return new Float(val.floatValue());
    case 7:
      return new Double(val.doubleValue());
    default:
      if (Debug.enabled)
        Debug.println("Attempt to narrow wrong primitive ("+clsID+").");
      return null;
    }
  };
};
