/*
 * $Id: Debug.java 490 2006-10-01 16:08:04Z metlov $
 *
 * Galaxy PBW server 2.0 abstract core package.
 *
 * Implementation of the Galaxy game server for playing over WWW. For more
 * information about Galaxy PBW development visit :
 *    http://kinetic.ac.donetsk.ua/galaxywww/
 *
 * (c) 1996 -- 2007 by Konstantin Metlov(metlov@kinetic.ac.donetsk.ua);
 *
 * Galaxy PBW server package is Distributed under GNU General Public License
 *
 *    This code comes with ABSOLUTELY NO WARRANTY.
 *  For license details see COPYING file in this directory.
 *
 */

package gnu.jel.debug;

/** 
 * This class used for incorporating internal checks and
 * assertions into the code.  
 * <BR>None of these functions does anything if Debug.enabled is false.
 * <BR>If you really want to throw ALL debug messages from the final,
 * compiler generated, code -- wrap calls to Debug methods into the
 * <TT>if</TT> statement, checking <TT>Debug.enabled</TT> constant.
 * As shown in the example : 
 * <PRE>
 * import cz.fzu.metlov.jel.*;
 * ..... BLA BLA BLA ...
 * if (Debug.enabled) {
 *  Debug.println("I want this message to disappear in the optimized version");
 *  Debug.check(foo==superTimeConsumingFunction(bar), 
 * "I do not want to evaluate superTimeConsumingFunction(), when optimized."); 
 * }; 
 *</PRE> 
 */
public final class Debug {

  /**
   * Determines if debugging is enabled in current compilation.
   */
  public final static boolean enabled=false; // <-- AUTO GENERATED
  

  /**
   * Prints a line of the debug output.
   * The resulting line goes to System.err and is prefixed by "[DEBUG] ".
   * @param message message to print.
   */
  public final static void println(String message) {
    if (enabled) {
      System.err.print("[DEBUG] ");
      System.err.println(message);
    };
  };

  /**
   * Checks for the condition.
   * If condition is false this function prints a given message
   * to the System.err along with the stack trace.
   * @param condition is the condition to check.
   * @param message is the message to print if condition is false.
   */
  public final static void check(boolean condition, String message) {
    if (enabled && (!condition)) {
      System.err.print("Assertion failed :");
      System.err.println(message);
      Throwable tracer=new Throwable(message);
      tracer.printStackTrace();
    }; 
  };

  /**
   * Checks for the condition.
   * If condition is false this function prints a "Assertion failed."
   * to the System.err along with the stack trace.
   * @param condition is the condition to check.
   */
  public final static void check(boolean condition) {
    if (enabled && (!condition)) {
      Throwable tracer=new Throwable("Assertion failed.");
      tracer.printStackTrace();
    }; 
  };

  /**
   * Reports an exception, which should not occur(i.e. handled improperly).
   * @param t is what was thrown.
   * @param message is algorithm specific message.
   */
  public final static void reportThrowable(Throwable t,String message) {
    if (enabled) {
      System.err.println("Unexpected exception has occured :");
      System.err.println(message);
      t.printStackTrace();
    };
  };

  /**
   * Reports an exception, which should not occur(i.e. handled improperly).
   * @param t is what was thrown.
   */
  public final static void reportThrowable(Throwable t) {
    if (enabled) {
      System.err.println("Unexpected exception has occured :");
      t.printStackTrace();
    };
  };
};

