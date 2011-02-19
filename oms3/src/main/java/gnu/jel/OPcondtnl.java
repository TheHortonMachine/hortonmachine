/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: OPcondtnl.java 490 2006-10-01 16:08:04Z metlov $
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
 * A tree node, representing conditional.
 */
public class OPcondtnl extends OP {
  /**
   * Creates conditional operator.
   * <P>On entry the paramOPs should contain <boolean> <result of the
   * 1st branch> <result of the 2nn branch>.
   * @param paramOPs stack holding the operands
   */
  public OPcondtnl(Stack<OP> paramOPs) 
    throws CompilationException {
    
    chi=new OP[3];
    for (int i=2;i>=0;i--)
      chi[i]=(OP)paramOPs.pop();

    int type2ID=chi[2].resID;
    Class type2=chi[2].resType;
    int type1ID=chi[1].resID;
    Class type1=chi[1].resType;
    int argID=chi[0].resID;

    if (unwrapType[argID]!=0) // first argument must be boolean
      throw new CompilationException(23,null);
    if (argID!=0) { // unwrap
      paramOPs.push(chi[0]);
      chi[0]=new OPunary(paramOPs,0,null,false);
    };

    // determine the result type according to JLS 15.24
    resID=-1;

    if ((type1ID>=8) && (type2ID>=8)) {
      // references
      if (isWidening(type1ID, type1, type2ID, type2)) {
        resID=type2ID;
        resType=type2;
      } else if (isWidening(type2ID, type2, type1ID, type1)) {
        resID=type1ID;
        resType=type1;
      };
      // otherwise both must unwrap to primitives, which is checked next
    };

    if (resID<0) {
      // if reference conversion did not work
      int type1IDunwrp;
      int type2IDunwrp;

      if ((resID=type1IDunwrp=unwrapType[type1ID])!=
          (type2IDunwrp=unwrapType[type2ID])) {
        if (((type1IDunwrp==1) && (type2IDunwrp==3)) || 
            ((type1IDunwrp==3) && (type2IDunwrp==1)))
          resID=3;
        else {
          if ((type1IDunwrp>=8) || (type2IDunwrp>=8) || 
              ((resID=OPbinary.promotions[type1IDunwrp][type2IDunwrp])<0)) {
            Object[] paramsExc={type1,type2};
            throw new CompilationException(24,paramsExc);
          };
        };
      };
      resType=specialTypes[resID]; // here it's always the primitive
    };
    
    // convert types
    if ((type1ID!=resID) || ((resID==8) && (type1!=null) && 
                             (type1!=resType))) {
      paramOPs.push(chi[1]);
      chi[1]=new OPunary(paramOPs,resID,resType,false);
    };
    
    if ((type2ID!=resID) || ((resID==8) && (type2!=null) && 
                             (type2!=resType))) {
      paramOPs.push(chi[2]);
      chi[2]=new OPunary(paramOPs,resID,resType,false);
    };
  };

  public void compile(ClassFile cf) {
    chi[0].compile(cf);
    if (chi[1]!=null) {
      // in the case condition was impossible to evaluate at compile-time
      cf.code(0xE4); // start "true" branch
      chi[1].compile(cf);
      cf.code(0xE5); // finish "true" branch / start "false" branch
      chi[2].compile(cf);
      cf.code(0xE6); // finish "false" branch
    };
  };

  public Object eval() throws Exception {
    boolean cond;
    try {
      cond=((Boolean)chi[0].eval()).booleanValue();
    } catch (Exception e) {
      try {
        chi[1]=new OPload(chi[1],chi[1].eval());
      } catch (Exception exc) {
      };
      try {
        chi[2]=new OPload(chi[2],chi[2].eval());
      } catch (Exception exc) {
      };
      throw e;
    };
    
    OP rop=cond?chi[1]:chi[2];
        
    try {
      return rop.eval();
    } catch (Exception e) {
      // if can't eval, but know the condition
      chi[0]=rop;
      chi[1]=null;
      chi[2]=null;
      throw e;
    }
  };

};
