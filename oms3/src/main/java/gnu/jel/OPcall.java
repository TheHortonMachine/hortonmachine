/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: OPcall.java 490 2006-10-01 16:08:04Z metlov $
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

import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import gnu.jel.debug.Debug;
import java.util.Stack;

/**
 * A tree node, representing a method call (field/local variable load).
 */
public class OPcall extends OP {

  /** Holds method to be executed */
  public Member m; // member to eval (null for the local variable access)

  /**
   * local variable number (in case m=null), number of formal
   * parameters of the method to call otherwise.  */
  public int nplv=0;

  /**
   * if evaluation of the method will be attempted at compile-time */
  private boolean aEval=false;

  /**
   * Prepares a new method/field call/get operation to be added to the code.
   * @param paramOPs stack holding the operands
   * @param m method/field to call/get.
   * @param aEval indicates if the method call should be attempted
   *                    at the compile time
   */
  public OPcall(Stack<OP> paramOPs, Member m, boolean aEval) 
    throws CompilationException {
    this.m=m;
    
    Class[] reqParamTypes=Library.getParameterTypes(m);
    nplv=reqParamTypes.length;
    
    int thisIdx=0;
    if ((m.getModifiers() & 0x0008) == 0) { // method is not static
      thisIdx=-1;
      nplv++;
    };

    this.chi=new OP[nplv];

    // convert formal and actual parameter types and "this", if needed    
    for(int i=reqParamTypes.length-1;i>=thisIdx;i--) {
      Class cReq=(i>=0?reqParamTypes[i]:m.getDeclaringClass());

      OP cop=(OP)paramOPs.peek();

      // add convert type OP
      if ((cop.resID==10) || (cop.resType!=cReq))
        paramOPs.push(new OPunary(paramOPs,typeID(cReq),cReq,i<0));

      chi[i-thisIdx]=(OP)paramOPs.pop();
    };
    
    // push & store the result type
    resType=Library.getType(m);
    resID=typeID(resType);
    //    System.out.println("MAKING CALL TO "+m.getName()+
    //                       ClassFile.getSignature(m)+" returning "+
    //                       m.getType());

    // determine if compile-time evaluation should be attempted
    this.aEval=(aEval &&  // eval only if requested.
                ((m.getModifiers() & 0x0008)!=0) && // if static
                ((resID<=7) || (resID==11)) //  if can store result
                );
  };

  /**
   * Prepares access to the local variable (formal parameter) of method.
   * @param lvarn local variable number.
   * @param type local variable type.
   */
  public OPcall(int lvarn, Class type) {
    this.m=null;
    this.nplv=lvarn;
    resID=typeID(type);
    resType=type;
  };

  /**
   * Attempts to evaluate this function.
   * @return the OPload, representing the function's result (if it can \
   *         be evaluated).
   * @throws Exception if the function can't be evaluated, or the evaluation
   *         results in error.
   */
  public Object eval() throws Exception {
    Object[] params=new Object[chi.length];
    boolean[] evaltd=new boolean[chi.length];
    
    Exception exception=null;
    for(int i=0;i<chi.length;i++) {
      try {
        params[i]=chi[i].eval();
          evaltd[i]=true;
      } catch (Exception exc) {
        exception=exc;
        evaltd[i]=false;
      };
    };
    
    Object res=null;
    
    if (exception==null) { // try to evaluate the method
      try {
        if (!aEval)
          throw new Exception();
        
        if (m instanceof Method) res=((Method)m).invoke(null,params);
        else if (m instanceof Field) res=((Field)m).get(null);
        else throw new Exception();       
      } catch (Exception exc) {
        exception=exc;
      };
    };
    
    if (exception!=null) {
      // didn't eval, replace at least evaluated children
      for(int i=0;i<chi.length;i++)
        if (evaltd[i])
          chi[i]=new OPload(chi[i],params[i]);
      throw exception;
    };
    
    return res;
  };
  
  // compilation code
  public void compile(ClassFile cf) {
    if (m==null) {
      // load the local variable with a given number

      //  int[][] load={
      //    //wide  shrt  0    1    2    3
      //    {0x15c4,0x15,0x1a,0x1b,0x1c,0x1d}, // Z
      //    {0x15c4,0x15,0x1a,0x1b,0x1c,0x1d}, // B
      //    {0x15c4,0x15,0x1a,0x1b,0x1c,0x1d}, // C
      //    {0x15c4,0x15,0x1a,0x1b,0x1c,0x1d}, // S
      //    {0x15c4,0x15,0x1a,0x1b,0x1c,0x1d}, // I
      //    {0x16c4,0x16,0x1e,0x1f,0x20,0x21}, // J
      //    {0x17c4,0x17,0x22,0x23,0x24,0x25}, // F
      //    {0x18c4,0x18,0x26,0x27,0x28,0x29}, // D
      //    {0x19c4,0x19,0x2a,0x2b,0x2c,0x2d}  // L
      //  };

      int lvt=resID-4;
      if (lvt<0) lvt=0;

      int lvarn_translated=cf.paramsVars[nplv];
      
      if (lvarn_translated<4) 
        cf.code(0x1a+lvt*4+lvarn_translated);
      else if (lvarn_translated<=255)
        cf.code(0x15+lvt+(lvarn_translated<<8));
      else {
        cf.code(((0x15+lvt)<<8)+0xc4);
        cf.writeShort(lvarn_translated);
      };
    } else {
      cf.code(0xFB); // labels block;

      for(int i=0;i<chi.length;i++) {
        chi[i].compile(cf);
        cf.code(0xFA); // ensure value in stack 
      };
      
      cf.code(0xF8); // labels unblock;

      for(int i=0;i<chi.length;i++)
        cf.noteStk(chi[i].resID,-1);        
      
      cf.codeM(m); // call the method / get field
    };
    cf.noteStk(-1,resID);
  };
  
};
