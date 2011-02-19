/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: CompilationException.java 490 2006-10-01 16:08:04Z metlov $
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
import java.text.MessageFormat;

/**
 * Represents an error encountered during the compilation.
 * <P> The text of the messages can be changed/internationalized by
 * modification of JEL.properties file
 */
@SuppressWarnings("serial") public class CompilationException extends Exception {
  
  public  int col=-1;
  private int code=-1; // error code
  private Object[] params=null; // parameters to generate messages

  /**
   * Constructs new CompilationException with a single formatting parameter.
   * @param code is the error code (must correspond to a message in
   *             JEL.properties file).
   * @param param is the single Object parameter to be used in message
   *             formatting.
   */
  public CompilationException(int code, Object param) {
    if (Debug.enabled)
      Debug.check(code>=0);

    this.code=code;
    Object[] temp={param};
    this.params=temp;
  };

  /**
   * Constructs new CompilationException with a several formatting parameter.
   * @param code is the error code (must correspond to a message in
   *             JEL.properties file).
   * @param params is an array of Objects, which are to be used in message
   *             formatting.
   */
  public CompilationException(int code, Object[] params) {
    if (Debug.enabled)
      Debug.check(code>=0);

    this.code=code;
    this.params=params;
  };

  /**
   * Used to obtain the column, where error have occurred.
   * @return column, where error have occurred.
   */
  public int getColumn() {
    return col;
  };

  /**
   * Used to obtain the error code.
   * @return the error code, corresponding to one of the messages in
   *         JEL.properties file.
   */
  public int getType() {
    return code;
  };

  /**
   * Used to obtain the parameters for this error.
   * @return the parameters to be used in message formatting, they provide
   *         further information about the error.
   */
  public Object[] getParameters() {
    return params;
  };

  /**
   * Used to obtain the formatted error message.
   * @return the formatted error message.
   */
  public String getMessage() {
    if (Debug.enabled)
      Debug.check(col>=0);
    return TableKeeper.getMsg(code,params);
  };

};


