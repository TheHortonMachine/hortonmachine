/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*-
 * $Id: Evaluator.java 490 2006-10-01 16:08:04Z metlov $
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

//import gnu.jel.generated.EC;
//import gnu.jel.generated.CharStream;
import gnu.jel.debug.Debug;
import java.lang.reflect.Member;

/**
 * This is the main frontend to JEL.
 * <P>It is intended for compilation of algebraic expressions involving 
 * functions.
 * <P> Syntax allows variables, which can be either a public fields of
 * certain objects or functions with no arguments.  If a method like
 * "<TT>double x() {}</TT>;" is defined in the dynamic library class
 * (see <tt>gnu.jel.Library</tt> documentation on how to do this), an
 * expression "<TT>sin(x)</TT>" will call the method "<TT>x()</TT>" (
 * and function <TT>Math.sin()</TT> ) each time it is evaluated.
 * Static methods in namespace are assumed to be stateless (unless
 * this default behaviour is explicitly overridden, as is necessary
 * for <tt>Math.random()</tt>) and will be called at compile time if
 * their arguments are known.
 * <P>It is possible to have any type of intermediate object
 * throughout the calculation as long as types of the function return
 * values and parameters stay compatible. The compiler can
 * do all the type conversions permissible in Java language and more 
 * (e.g. between reflection wrappers <tt>java.lang.Integer</tt>,... and
 * primitives). Widening type conversions (not leading to precision loss)
 * are applied by JEL automatically, narrowing should be explicitly 
 * requested.
 * <P>There is variant of the "compile" function with three arguments,
 * which allows to fix the type of the expression result. For example:
 * <PRE>
 * CompiledExpression expression=compile("2*6+6",lib,Double.TYPE); 
 * </PRE> 
 * will produce a compiled expression, whose return type is always
 * <TT>double</TT>. For additional information on how to use this
 * feature to eliminate object allocation overhead see
 * <TT>gnu.jel.CompiledExpression</TT> documentation.
 * 
 * <P> Care should be taken during the assembly of static and dynamic libraries
 * to avoid conflicts and unexpected return types.
 *
 * <P>(c) 1998-2003, by Konstantin Metlov<BR>
 * Prague, CZ
 * @see gnu.jel.CompiledExpression
 * @see gnu.jel.Library
 */
public class Evaluator {

  protected static ClassFile cf_orig;
  protected static int retID_patchback=0;
  protected static int retIDC_patchback=0;
  protected static LocalMethod[] eval_methods= new LocalMethod[10];

  static {
    try {
      // prepare eval methods
      Class[] paramsE=new Class[1];
      paramsE[0]=(new Object[0]).getClass();
      Class[] excptnsE=new Class[1];
      excptnsE[0]=Class.forName("java.lang.Throwable");

      for(int i=0;i<10;i++) {
        String name="evaluate";
        Class cls=OP.specialTypes[i];
        if (i!=8) 
          name=name+'_'+cls;
        else 
          cls=(new Object()).getClass();
        eval_methods[i]=new LocalMethod(0x0001,cls,name,paramsE,excptnsE);
      };
      
      Class cmplExpr=Class.forName("gnu.jel.CompiledExpression");
      ClassFile cf=new ClassFile(0x0001,"dump",cmplExpr,null,null);
      // public 
      LocalMethod cnstr=
        new LocalMethod(0x0001,Void.TYPE,"<init>",null,null);
      cf.newMethod(cnstr,null);
      cf.code(0x2a);                //| aload_0  ;loads "this"
      cf.noteStk(-1,11); // not important what, it must be a reference
      cf.codeM(cmplExpr.getConstructor(new Class[0]));
      cf.noteStk(11,-1);
      cf.code(0xb1);                //| return void
      
      LocalMethod getType=
      new LocalMethod(0x0001,Integer.TYPE,"getType",null,null);
      cf.newMethod(getType,null);
      cf.code(0x10);                //| bipush
      retID_patchback=cf.tsize;
      cf.code(8);                   //    type placeholder
      cf.noteStk(-1,4); // note "int"
      
      cf.code(0xAC);                //| ireturn
      cf.noteStk(4,-1); // rm "int"

      Class clazz=Class.forName("java.lang.Class");
      Class[] paramsC=new Class[1];
      paramsC[0]=Class.forName("java.lang.String");
      java.lang.reflect.Method forName=clazz.getMethod("forName",paramsC);
      Class[] excptnsC=new Class[1];
      excptnsC[0]=Class.forName("java.lang.ClassNotFoundException");

      LocalMethod getTypeC=
      new LocalMethod(0x0001,clazz,"getTypeC",null,excptnsC);
      cf.newMethod(getTypeC,null);
      cf.code(0x13);                //| ldc_w
      retIDC_patchback=cf.tsize;
      cf.writeShort(0);             //| Cp index to the class name
      cf.noteStk(-1,11);            // note string load
      cf.codeM(forName);            //| Class.forName
      cf.noteStk(11,8);             // string replaced by "class"
      cf.code(0xB0);                //| areturn
      cf.noteStk(8,-1);             // rm "class"

      cf_orig=(ClassFile)cf.clone();
    } catch (Exception exc) {
      if (Debug.enabled) Debug.reportThrowable(exc);
    };
  };
  
  /**
   * Compiles expression, resolving the function names in the library.
   * @param expression is the expression to compile. i.e. "sin(666)" .
   * @param lib Library of functions exported for use in expression.
   * @param resultType identifies the type result should be converted to. Can
   *        be null, in this case the result type is not fixed.
   * @return Instance of the CompiledExpression subclass, implementing
   *   the specified expression evaluation.
   * @exception gnu.jel.CompilationException if the expression is not
   *  syntactically or semantically correct.
   * @see gnu.jel.CompiledExpression
   */
  public static CompiledExpression compile(String expression, Library lib,
                                           Class resultType) 
    throws CompilationException {
    byte[] image=compileBits(expression,lib,resultType);
    try {
      return (CompiledExpression)(ImageLoader.load(image)).newInstance();
    } catch (Exception exc) {
      if (Debug.enabled)
        Debug.reportThrowable(exc);
      return null;
    }
  };
  
  /**
   * Compiles expression, resolving the function names in the library.
   * <P>This variant of compile allows to store expressions in a 
   * <TT>java.io.OutputStream</TT> using Java serialization mechanism.
   * @param expression is the expression to compile. i.e. "sin(666)" .
   * @param lib Library of functions exported for use in expression.
   * @param resultType identifies the type result should be converted to. Can
   *        be null, in this case the result type is not fixed.
   * @return Byte array, representing the expression
   * in a standard Java classfile format. It can be conveniently 
   * loaded (with renaming, if necessary)
   * into Java VM using <tt>gnu.jel.ImageLoader</tt>.
   * @exception gnu.jel.CompilationException if the expression is not
   *  syntactically or semantically correct.
   * @see gnu.jel.CompiledExpression
   * @see gnu.jel.ImageLoader
   */
  public static byte[] compileBits(String expression, Library lib,
                                   Class resultType) 
    throws CompilationException {
    OP code=parse(expression,lib,resultType);
    
    // Perform constants folding
    try {
      code=new OPload(code,code.eval());
    } catch (Exception exc) {
    };

    return getImage(code);
  };

  /**
   * Compiles expression, resolving the function names in the library.
   * @param expression is the expression to compile. i.e. "sin(666)" .
   * @param lib Library of functions exported for use in expression.
   * @return Instance of the CompiledExpression subclass, implementing
   *   the specified expression evaluation.
   * @exception gnu.jel.CompilationException if the expression is not
   *  syntactically or semantically correct.
   * @see gnu.jel.CompiledExpression
   */
  public static CompiledExpression compile(String expression, Library lib)
    throws CompilationException {
    return compile(expression, lib, null);
  };

  /**
   * Compiles expression, resolving the function names in the library.
   * <P>This variant of compile allows to store expressions in a 
   * <TT>java.io.OutputStream</TT> using Java serialization mechanism.
   * @param expression is the expression to compile. i.e. "sin(666)" .
   * @param lib Library of functions exported for use in expression.
   * @return Byte array, representing the expression
   * in a standard Java classfile format. It can be conveniently 
   * loaded (with renaming, if necessary)
   * into Java VM using <tt>gnu.jel.ImageLoader</tt>.
   * @exception gnu.jel.CompilationException if the expression is not
   *  syntactically or semantically correct.
   * @see gnu.jel.CompiledExpression
   * @see gnu.jel.ImageLoader
   */
  public static byte[] compileBits(String expression, Library lib)
    throws CompilationException {
    return compileBits(expression, lib, null);
  };

  static OP parse(String expression, Library lib,
                                Class resultType) throws CompilationException {
    return (new Parser(expression,lib)).parse(resultType);
    //    return (new EC(new CharStream(expression))).parse(resultType,lib);
  };

  static byte[] getImage(OP code) {
    int retID=code.resID;

    ClassFile cf=(ClassFile)cf_orig.clone();

    // set return type
    int otsize=cf.tsize;
    cf.tsize=retID_patchback;
    cf.write((byte)retID);
    cf.tsize=otsize;

    String retName;
    if ((retID<8))
      retName=OP.specialTypes[20+retID].getName();
    else if (retID==9)
      retName="java.lang.Void";
    else 
      retName=code.resType.getName();

    // set return class
    otsize=cf.tsize;
    cf.tsize=retIDC_patchback;
    cf.writeShort(cf.getIndex(retName,8));
    cf.tsize=otsize;
    
    // add the evaluate method
    cf.newMethod(eval_methods[retID],null);
    code.compile(cf);

    return cf.getImage();
  };
  
};











