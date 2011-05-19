/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
/*
 * $Id: CompiledExpression.java 490 2006-10-01 16:08:04Z metlov $
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

/**
 * This abstract class is a superclass of every JEL-compiled expression,
 * each of which overrides some of the abstract methods below.
 * <p> Most methods of this class accept a reference to the array of objects. 
 * This reference is a pointer to the dynamic object library. As you know,
 * JEL allows to call virtual methods of Java classes, but, as you also know,
 * virtual methods require a reference to an object instance
 * (<tt>this</tt>) to be
 * passed along with parameters. The array <tt>dl</tt> (dynamic 
 * library) serves just this purpose. It should contain references
 * to objects of all classes, whose 
 * <i>virtual</i> methods were put into the Library of callable functions.
 * Objects
 * in the <tt>dl</tt> array should correspond one-to-one to classes
 * in the array, passed as a second argument of <TT>gnu.jel.Library</TT>
 * constructor.
 * <p>
 * <p> There are two ways of evaluating the compiled expressions allowing
 * to compromise between raw performance and simplicity:
 * <p> The <i>first method</i> (simplest one) is based on the call to the
 * <TT> evaluate</TT> method, which will
 * return an object even if the result of computation was of a primitive type.
 * The primitive types will be automatically converted into the 
 * corresponding reflection 
 * objects. There is certain overhead, associated with object creation, it 
 * takes CPU cycles and also produces load on the garbage collector later..
 * <P>For massive (thousand times) evaluations of functions, producing 
 * results of primitive Java types, the <I>second 
 * method</I> can be more suitable. It is based on : first, determining the 
 * type of the result, and, then, subsequent call to the
 * <U>corresponding</U> evaluateXXX method.
 * <P> The type of the resulting expression can be determined by call to the
 * getType() method. It will return an integer number, indentifying the type,
 * <U>proper</U> evaluateXXX method can be determined, 
 * based on the following table:
 * <PRE>
 * getType()   |  method to call
 * ------------+----------------------
 * 0           | evaluate_boolean(...)
 * 1           | evaluate_byte(...)
 * 2           | evaluate_char(...)
 * 3           | evaluate_short(...)
 * 4           | evaluate_int(...)
 * 5           | evaluate_long(...)
 * 6           | evaluate_float(...)
 * 7           | evaluate_double(...)
 * 8           | evaluate(...)         <- result is Object (universal method)
 * 9           | evaluate_void(...)
 * -----------------------------------
 * </PRE>
 * <P> Note: If a wrong evaluateXXX() method is called, it will return zero,
 * and, in debug version of JEL (jel_g.jar) only, a warning will be 
 * printed to stderr.
 * <P> There is a possibility to enforce resulting type of the expression at
 * compile time (see <tt>gnu.jel.Evaluator</tt>). 
 * Use it to avoid unnecesary type checks.
 * @see gnu.jel.Library
 * @see gnu.jel.Evaluator
 */
public abstract class CompiledExpression {
  
  /**
   * Returns type of the expression result.
   * <P> The type is encoded in integer. Following table could help
   * in determining what it is :
   * <PRE>
   * getType()   |  method to call
   * ------------+-----------------
   * 0           | boolean
   * 1           | byte
   * 2           | char
   * 3           | short
   * 4           | int
   * 5           | long
   * 6           | float
   * 7           | double
   * 8           | Object
   * 9           | void
   * </PRE>
   * <P> The reason not to introduce the family of, say, TYPE_XXX constants
   * is to save space. There are so many things connected with these
   * particular numbers in code generator that it could be a pain to change 
   * their meaning. Also, this shoul never be necessary because these 
   * are ALL Java 1.X primitive types.
   * @return the type of the expression, encoded in integer.
   */
  public abstract int getType();

  /**
   * Returns the type of the expression result.
   * <p>If the result has primitive type the corresponding wrapper
   * class is returned (please note that it is just a wrapper class like
   * java.lang.Integer instead of exact primitive type class 
   * java.lang.Integer.TYPE). This corresponds to the wrapping done
   * in non-specialized evaluate() method.
   * <p>When the result is an object, the returned value is the most 
   * specific reference to the class of that object (which is always
   * a subclass of the returned class) available at compile time.
   * <p>The precision of determining the result type this way mainly
   * depends on the design of the user's function namespace (defined
   * by gnu.jel.Library class). It is possible to design a library in
   * such a way that the best approximation to the result type obtainable
   * at compile time would be the java.lang.Object class, which will
   * be returned by this method, thus, providing no useful information
   * about the actual type.
   * <p>Please note again that the guaranteed exact type of the result can't be
   * determined at compile time and can be quiered only after evaluating
   * the expression (directly from resulting object). 
   * <p> The only guarantee
   * this method provides is that a variable of the returned expression type
   * can be assigned by a reference, resulting from a call to evaluate.
   */
  public abstract Class getTypeC();
  
  /**
   * Evaluates the expression, representing result as an object.
   * <P> If the result of evaluation is Java primitive type it gets wrapped
   * into corresponding reflection object , i.e. 
   * <TT>int</TT> -> <TT>java.lang.Integer</TT>,
   * <TT>boolean</TT> -> <TT>java.lang.Boolean</TT>,...
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @return the result fo computation.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public Object evaluate(Object[] dl) throws Throwable {
    int type=getType();
    Object res=null;
    switch (type) {
    case 0: res=new Boolean(evaluate_boolean(dl)); break;
    case 1: res=new Byte(evaluate_byte(dl)); break;
    case 2: res=new Character(evaluate_char(dl)); break;
    case 3: res=new Short(evaluate_short(dl)); break;
    case 4: res=new Integer(evaluate_int(dl)); break;
    case 5: res=new Long(evaluate_long(dl)); break;
    case 6: res=new Float(evaluate_float(dl)); break;
    case 7: res=new Double(evaluate_double(dl)); break;
    case 9: evaluate_void(dl); break;
    default:
      if (Debug.enabled)
	Debug.check(false,"WrongTypeReturned from "+
		     "CompiledExpression.getType().");
    };
    return res;
  };

  /**
   * Evaluates the expression whose result has type <TT>boolean</TT>.
   * <P> If the type of the result is not a <TT>boolean</TT> this function 
   * returns always <TT>false</TT>, debug version will print a warning to stderr.
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @return the result fo computation.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public boolean evaluate_boolean(Object[] dl) throws Throwable {
    if (Debug.enabled)
      Debug.println("Wrong evaluateXXXX() method called,"+
		    " check value of getType().");
    return false;
  };

  /**
   * Evaluates the expression whose result has type <TT>byte</TT>.
   * <P> If the type of the result is not a <TT>byte</TT> this function returns
   * always <TT>0</TT>, debug version will print a warning to stderr.
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @return the result fo computation.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public byte evaluate_byte(Object[] dl) throws Throwable {
    if (Debug.enabled)
      Debug.println("Wrong evaluateXXXX() method called,"+
		    " check value of getType().");
    return 0;
  };

  /**
   * Evaluates the expression whose result has type <TT>short</TT>.
   * <P>If the type of the result is not a <TT>short</TT> this function returns
   * always <TT>0</TT>, debug version will print a warning to stderr.
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @return the result fo computation.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public short evaluate_short(Object[] dl) throws Throwable {
    if (Debug.enabled)
      Debug.println("Wrong evaluateXXXX() method called,"+
		    " check value of getType().");
    return 0;
  };

  /**
   * Evaluates the expression whose result has type <TT>char</TT>.
   * <P>If the type of the result is not a <TT>char</TT> this function returns
   * always <TT>'?'</TT>, debug version will print a warning to stderr.
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @return the result fo computation.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public char evaluate_char(Object[] dl) throws Throwable {
    if (Debug.enabled)
      Debug.println("Wrong evaluateXXXX() method called,"+
		    " check value of getType().");
    return '?';
  };

  /**
   * Evaluates the expression whose result has type <TT>int</TT>.
   * <P>If the type of the result is not a <TT>int</TT> this function returns
   * always <TT>0</TT>, debug version will print a warning to stderr.
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @return the result fo computation.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public int evaluate_int(Object[] dl) throws Throwable {
    if (Debug.enabled)
      Debug.println("Wrong evaluateXXXX() method called,"+
		    " check value of getType().");
    return 0;
  };

  /**
   * Evaluates the expression whose result has type <TT>long</TT>.
   * <P>If the type of the result is not a <TT>long</TT> this function returns
   * always <TT>0</TT>, debug version will print a warning to stderr.
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @return the result fo computation.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public long evaluate_long(Object[] dl) throws Throwable {
    if (Debug.enabled)
      Debug.println("Wrong evaluateXXXX() method called,"+
		    " check value of getType().");
    return 0L;
  };

  /**
   * Evaluates the expression whose result has type <TT>float</TT>.
   * <P>If the type of the result is not a <TT>float</TT> this function returns
   * always <TT>0.0</TT>, debug version will print a warning to stderr.
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @return the result fo computation.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public float evaluate_float(Object[] dl) throws Throwable {
    if (Debug.enabled)
      Debug.println("Wrong evaluateXXXX() method called,"+
		    " check value of getType().");
    return 0.0F;
  };

  /**
   * Evaluates the expression whose result has type <TT>double</TT>.
   * <P>If the type of the result is not a <TT>double</TT> this function 
   * returns always <TT>0.0</TT>, debug version will print a warning to stderr.
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @return the result fo computation.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public double evaluate_double(Object[] dl) throws Throwable {
    if (Debug.enabled)
      Debug.println("Wrong evaluateXXXX() method called,"+
		    " check value of getType().");
    return 0.0D;
  };

  /**
   * Evaluates the expression whose result has type <TT>void</TT>.
   * <P>If the type of the result is not a <TT>void</TT> debug version
   * will print a warning to stderr.
   * @param dl Array of the instance references to the objects in dynamic
   *  library. See description of this class above for more details.
   * @exception Throwable if any runtime error have occured (i.e. division
   *          by 0)
   * @see gnu.jel.Evaluator#compile
   * @see gnu.jel.Library
   */
  public void evaluate_void(Object[] dl) throws Throwable {
    if (Debug.enabled)
      Debug.println("Wrong evaluateXXXX() method called,"+
                    " check value of getType().");
    return;
  };

  

  // String and object comparisons
  private static java.text.Collator collator=null;
  
  public static int compare(String s1,String s2) {
    if (collator==null) 
      collator = java.text.Collator.getInstance();
    return collator.compare(s1,s2);
  };

};






