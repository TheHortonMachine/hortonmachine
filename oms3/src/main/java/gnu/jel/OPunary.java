/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: OPunary.java 490 2006-10-01 16:08:04Z metlov $
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
 * A tree node, representing unary operation.
 */
public class OPunary extends OP {

  /** code of this operation */
  public int code;

  private int uwrpCode=0; // what to code to unwrap the object
  private int implCode=0;
  private int uwrpsTo=-1;

  // The full list of codes is :
  // 0  -- negation (applicable to anything except boolean)
  // 1  -- bitwise not (applicable to all integral types)
  // 2  -- logical not (applicable to booleans only)
  // 3  -- return
  // 4  -- convert to boolean
  // 5  -- convert to byte
  // 6  -- convert to char
  // 7  -- convert to short
  // 8  -- convert to int
  // 9  -- convert to long
  // 10  -- convert to float
  // 11 -- convert to double
  // 12 -- convert to object (in this case the cls parameter gives class)
  // 13 -- convert to void (throw from stack)
  // 14 -- convert string to temporary string buffer (TSB)
  // 15 -- convert temporary string buffer (TSB) to string

  /** unary promotions of base types */
  protected final static byte[] unary_prmtns;
  
  private final static int[][] una;

  private final static String[] opNames;

  static {
    unary_prmtns=(byte[])TableKeeper.getTable("unary_prmtns");
    una=(int[][])TableKeeper.getTable("una");
    opNames=(String[])TableKeeper.getTable("opNames");

    if (Debug.enabled)
      Debug.check((opNames.length==una.length));
  };

  /**
   * Constructs a new unary operation.
   * <P>Codes are following:
   * <PRE>
   * 0  -- negation (applicable to anything except boolean)
   * 1  -- bitwise not (applicable to all integral types)
   * 2  -- logical not (applicable to booleans only)
   * 3  -- return the type in stack
   * </PRE>
   * @param paramOPs stack holding the operands
   * @param code operation code
   */
  public OPunary(Stack paramOPs, int code) 
    throws CompilationException {
    if (Debug.enabled)
      Debug.check((code>=0) && (code<=3));

    this.code=code;
    chi=new OP[1];
    chi[0]=(OP)paramOPs.pop();
    
    int opID=chi[0].resID;
    Class opType=chi[0].resType;

    if (code==3) { // return
      // everything can be returned no checks/unwraps needed
      resID=(opID>9?8:opID);    // computes base type
      resType=opType;
      implCode=una[code][resID];
    } else {
      
      // unwrap object if can
      int unwrpID=unwrapType[opID];      
      if (unwrpID!=opID) {
        uwrpCode=((opID-12+11)<<8)+0x00FE;
        uwrpsTo=unwrpID;
      };
      
      if ((implCode=una[code][unwrpID])==0xFF) {
        // operation is not defined on types
        Object[] paramsExc={opNames[code],opType};
        throw new CompilationException(28,paramsExc);
      };
      
      resID=unary_prmtns[unwrpID];
      resType=specialTypes[resID];
    };
    
  };

  /**
   * Creates conversion operation to the given class.
   * @param paramOPs stack holding the operands
   * @param targetID ID of primitive type to convert to.
   * @param targetClass the class to convert to, in case cldID=8
   * @param allownarrowing if narrowing conversions are allowed.
   */
  public OPunary(Stack paramOPs, int targetID, Class targetClass,
                 boolean allownarrowing) throws CompilationException {
    if (Debug.enabled) {
      // check for proper target type was identification
      Debug.check(((targetID==8)^(targetClass==null)) ||
                   ((targetID!=8)&&
            (specialTypes[targetID].isAssignableFrom(targetClass)))
                   );
      
      if (targetID==8) {
        Debug.check(typeID(targetClass)==targetID,
                     "The type was improperly identified for OPunary conv.");
      };
    };

    // set the result type
    resID=targetID;
    resType=(targetClass!=null?targetClass:specialTypes[targetID]);

    chi=new OP[1];
    chi[0]=(OP)paramOPs.pop();

    Class currClass=chi[0].resType;
    int   currID=chi[0].resID;

    int unwrappedCurrID=unwrapType[currID];

    // there are folowing cases:
    // unwrappable object to primitive
    // string-like to string
    // string or string-like to TSB
    // TSB to string
    // primitive to primitive
    // object to object 
    // error (object -> primitive, primitive->object)

    code=4+resID;
    if ((resID>=8) && (resID!=10) && (currID!=10) && 
        (!((currID==28) && ((resID==10) || (resID==11))))) {
      // object to object
      code=4+8;
      unwrappedCurrID=8;
    } else {
      // unwrappable object to primitive
      // string or string-like to TSB
      // TSB to string
      // primitive to primitive
      if (unwrappedCurrID!=currID) {
        uwrpCode=((currID-12+11)<<8)+0x00FE;
        uwrpsTo=unwrappedCurrID;
      };
    };

    if ((implCode=una[code][unwrappedCurrID])==0xFF) {
      //Debug.println("code="+code);
      // can't convert at all
      Object[] paramsExc={currClass,resType};
      throw new CompilationException(21,paramsExc);
    };

    if (!(allownarrowing || 
          isWidening(currID,currClass,resID,resType))) {
      // can't do narrowing conversions automatically
      Object[] paramsExc={currClass,resType};
      throw new CompilationException(22,paramsExc);
    };
  };

  public void compile(ClassFile cf) {
    if (code==2) cf.code(0xFB); // equivalent to cf.labels_block();
    if (code==14) {
      cf.code(0x0001FE591DFDBBL); // ANY => TSB
      //                      | new
      //                          <CP: java.lang.StringBuffer>
      //                      | dup
      //                      | invokespecial StringBuffer()
      cf.noteStk(-1,10); // pushes ref to TSB
    };
    
    chi[0].compile(cf);
    
    cf.code(uwrpCode); // unwrap object if needed
    
    if (uwrpsTo>=0)
      cf.noteStk(chi[0].resID,uwrpsTo); // note the unwrapping on stack

    cf.code(implCode);

    if (code==12) // code CP index for conversion to reference
      cf.writeShort(cf.getIndex(resType,9));
    
    // get rid of this switch is the task for the NEXT major update
    switch(code) {
    case 2:  // logical inversion does not change stack
    case 4:  // conversion to boolean does not change stack as well
      //     if it was a jump, jump is preserved  
      break;
    case 3:
    case 13:
      cf.noteStk(resID,-1); // return and (void) throw one word, add nothing
      break;
    case 14:
      cf.noteStk(11,-1);
      break;
    case 1: // bitwise not may have excess item on stack
      cf.noteStk(-1,resID);
      cf.noteStk(resID,-1);
    default: // other ops throw one word replace it by another
      cf.noteStk(uwrpsTo>=0?uwrpsTo:chi[0].resID,resID);
    };
  };

  public Object eval() throws Exception {
    Object operand=chi[0].eval();
    int operand_resID=chi[0].resID;

    if ((code==3) ||
        (code==13) ||
        (code==12)) { // do not evaluate, just replace operand
      chi[0]=new OPload(chi[0],operand);
      throw new Exception(); // bail out
    };

    if (code==2) { // logical not
      if (((Boolean)operand).booleanValue())
        operand=Boolean.FALSE;
      else
        operand=Boolean.TRUE;
    } else if (code<2) {
      Number val=widen(operand,operand_resID);
      switch(code) {
      case 0:  // negation
        if (operand_resID>5)
          val=new Double(-val.doubleValue());
        else
          val=new Long(-val.longValue());
        break;
      case 1:  // bitwise complement
        val=new Long(~val.longValue());
        break;
      default:
        if (Debug.enabled)
          Debug.check(code>=0,"Wrong unary opcode.");
      };
      operand=narrow(val,resID);
    } else {
      // conversion operations
      if (code==14) { // ANY->TSB
        operand=new StringBuffer(String.valueOf(operand));
      } else if (code==15) { // TSB->STR
        operand=operand.toString();
      } else {
        operand=narrow(widen(operand,operand_resID),resID);
      };
    };
    return operand;
  };
};

