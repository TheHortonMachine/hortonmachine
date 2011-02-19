/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: LocalMethod.java 490 2006-10-01 16:08:04Z metlov $
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
 * Represents a method local to the class being compiled.
 */
public class LocalMethod extends LocalField {
  private Class[] paramTypes;
  private Class[] exceptions;
    
  /**
   * Constructs a new local method.
   * @param modifiers sum of one or more of <TT>PUBLIC</TT>, <TT>PRIVATE</TT>,
   *                  <TT>PROTECTED</TT>,<TT>STATIC</TT>, <TT>FINAL</TT>,
   *                  <TT>SYNCHRONIZED</TT>, <TT>NATIVE</TT>, <TT>ABSTRACT</TT>
   *                  constants of java.lang.reflect.Modifier .
   * @param type type of the return value.
   * @param name name of the method
   * @param paramTypes array of types of formal parameters excluding "this"
   *                   (null means no parameters).
   * @param exceptions checked exceptions thrown
   */
  public LocalMethod(int modifiers, Class type, java.lang.String name, 
                     Class[] paramTypes,Class[] exceptions) {
	super(modifiers,type,name,null);

    if (paramTypes!=null)
      this.paramTypes=paramTypes;
    else
      this.paramTypes=new Class[0];
 
    if (exceptions!=null)
      this.exceptions=exceptions;
    else
      this.exceptions=new Class[0];
  };

  /**
   * Used to obtain types of formal parameters of this method.
   * @return array of classes representing formal parameters of the
   *         method except "this"
   */
  public Class[] getParameterTypes() {
	return paramTypes;
  };

  /**
   * Used to get checked exceptions thrown by this method
   * @return array of checked exceptions
   */
  public Class[] getExceptionTypes() {
    return exceptions;
  };

};

