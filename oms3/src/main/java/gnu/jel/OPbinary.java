/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 2 -*- 
 * $Id: OPbinary.java 490 2006-10-01 16:08:04Z metlov $
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
 * A tree node, representing binary operation.
 */
public class OPbinary extends OP {

  /** code of this operation */
  public int code;

  /** index into ops array to get code for this op. */
  private int opsIDX; 

  // Names of binary operations by ID in the readable form.
  private final static String[] opNames;

  /** binary promotions of base types */
  protected final static byte[][] promotions;

  // code chunks to implement the ops
  private final static  int[][] ops;
  
  // type of operands promotion by the opcode
  private static final byte[] promotionTypes;

  static {
    promotions=(byte[][])TableKeeper.getTable("promotions");
    ops=(int[][])TableKeeper.getTable("ops");
    promotionTypes=(byte[])TableKeeper.getTable("promotionTypes");
    opNames=(String[])TableKeeper.getTable("binOpNames");
  };

  /**
   * Constructs a new binary operation.
   * <P>Codes are following:
   * <PRE>
   * 0   --  addition
   * 1   --  substraction
   * 2   --  multiplication
   * 3   --  division
   * 4   --  remainder
   * 5   --  bitwise AND
   * 6   --  bitwise OR
   * 7   --  bitwise and logical XOR
   * 8   --  comparizon for equality
   * 9   --  comparizon for non-equality
   * 10  --  comparizon for "less" <
   * 11  --  comparizon for "greater or equal" >=
   * 12  --  comparizon for "greater" >
   * 13  --  comparizon for "less or equal" <=
   * 14  --  bitwise left shift <<
   * 15  --  bitwise right signed shift >>
   * 16  --  bitwise right unsigned shift >>>
   * 17  --  logical conjunction operator (AND)
   * 18  --  logical disjunction operator (OR)
   * 19  --  array element access operation
   * 20  --  reserved (used internally for string concatenation)
   * </PRE>
   * @param paramOPs stack holding the operands
   * @param opcode is the operation code
   */
  public OPbinary(Stack<OP> paramOPs, int opcode) throws CompilationException {
    if (Debug.enabled)
      Debug.check((opcode>=0) && (opcode<opNames.length));

    // get the operands types
    chi=new OP[2];
    chi[1]=(OP)paramOPs.pop();
    chi[0]=(OP)paramOPs.pop();

    int op2ID=chi[1].resID;
    Class op2Type=chi[1].resType;
    int op1ID=chi[0].resID;
    Class op1Type=chi[0].resType;

    // separate out string concatenation
    if (((op1ID==10) || (unwrapType[op1ID]==11)) && (opcode==0))
      opcode=20;

    this.code=opcode;
    //    Debug.println("opcode="+opcode+" op1ID="+op1ID+" op2ID="+op2ID);

    // perform the promotion of operands and determine the index
    // variables opsIDX, resID, optionally resType (if resID=8) and 
    // the following will be defined
    int op2cvtID=op2ID,op1cvtID=op1ID;
    
    resID=-1;
    boolean second_narrowing=false;
    int op1IDuwrp=unwrapType[op1ID];
    int op2IDuwrp=unwrapType[op2ID];

    switch (promotionTypes[opcode]) {
    case 0: // binary promotion with boolean result
      resID=0;
    case 1: // binary promotion
      op1cvtID=op2cvtID=opsIDX=
        promotions[op1IDuwrp][op2IDuwrp];
      if (resID<0) resID=opsIDX;

      if (opsIDX==-1) { // types are incompatible (can't promote)
        Object[] paramsExc={op1Type,op2Type,opNames[opcode]};
        throw new CompilationException(15,paramsExc);
      };

      break;
    case 3: // array promotion
      resType=(op1Type!=null?op1Type.getComponentType():null);
      if (resType==null)
        throw new CompilationException(18,null);
      resID=typeID(resType);
      opsIDX=(resID>=8?8:resID);
    case 2: // unary promotion of the first operand, second to int
      if (resID<0) {
        resID=op1cvtID=opsIDX=
          OPunary.unary_prmtns[op1IDuwrp];
        second_narrowing=true;
      };

      // integral types mask
      //      7 6 5 4 3 2 1 0
      //      0 0 1 1 1 1 1 0 = 0x3E
      if (((0x3E>>op2IDuwrp) & 1)==0) { // type is not integral
        Object[] paramsExc={opNames[opcode],op2Type};
        throw new CompilationException(27,paramsExc);
      };
      op2cvtID=4;
      break;
    case 4: // string concatenation promotion
      opsIDX=(op2ID>11?8:op2ID);
      op1cvtID=10; // TSB
      resID=10;  // TSB
      break;
    default:
      if (Debug.enabled)
        Debug.println("Wrong promotion type for binary OP "+
                      promotionTypes[opcode]);
    };
    
    // check if the OP can be implemented
    if (ops[opcode][opsIDX]==0xFF) { // operation is not defined on types
      Object[] paramsExc={opNames[opcode],op1Type,op2Type};
      throw new CompilationException(16,paramsExc);
    };
    
    // insert type conversion opcode
    if ((op1ID!=op1cvtID) && (op1cvtID!=8)) {
      paramOPs.push(chi[0]);
      chi[0]=new OPunary(paramOPs,op1cvtID,null,op1cvtID==10);
      //                                        can narrow to TSB
    };

    if ((op2ID!=op2cvtID) && (op2cvtID!=8)) {
      paramOPs.push(chi[1]);
      chi[1]=new OPunary(paramOPs,op2cvtID,null,second_narrowing);
    };

    if (resID!=8)
      resType=specialTypes[resID];
  };
 
  public void compile(ClassFile cf) {
    if ((code==17) || (code==18)) {
      chi[0].compile(cf);
      cf.code(code-(17 - 0xF4)); // logical_param AND/OR
      chi[1].compile(cf);
      cf.code(code-(17 - 0xF6)); // logical_end AND/OR
    } else {
      chi[0].compile(cf);
      cf.code(0xFA); //   ensure value;
      chi[1].compile(cf);
      cf.code(0xFA); //   ensure value;

      cf.code(ops[code][opsIDX] & 0xFFFFFFFFL); 
      // & is needed to prevent sign extension when converting "int" ops
      // element into "long" argument of code.
      
      cf.noteStk(chi[0].resID,-1);
      cf.noteStk(chi[1].resID,-1);
      
      if (cf.currJump==0) // jumps do not load anything to the stack
        cf.noteStk(-1,resID);
    };
  };

  public Object eval() throws Exception {
    
    Object c1w=null;
    boolean ep1=false;
    int c1ID=chi[0].resID;
    Object c2w=null;
    boolean ep2=false;
    int c2ID=chi[1].resID;
    
    try {
      c1w=chi[0].eval();
    } catch (Exception e) {
      ep1=true;
    };
    
    try {
      c2w=chi[1].eval();
    } catch (Exception e) {
      ep2=true;
    };

    try {
      if (ep1 || ep2 || (code==19)) // array access can't be evaluated.
        throw new Exception();
      
      if (code==20) {
        ((StringBuffer)c1w).append(String.valueOf(c2w));
      } else if ((c1ID>=8) || (c2ID>=8)) {
        // the only way the objects appear in this context
        // are the comparisons
        if (Debug.enabled) {
          Debug.check((code>=8)||(code<=13),
                       "only comparisons and concatenation binops can "+
                       "operate on objects.");
          Debug.check(c1ID!=10, "No TSB in this context 1");
          Debug.check(c1ID!=10, "No TSB in this context 2");
        };

        // only string literal comparisons are interpreted
        if ((c1ID!=11) || (c2ID!=11))
          throw new Exception();

        // compare the strings
        int res=CompiledExpression.compare((String)c1w,(String)c2w);
        c1w=((0x3172A>>>(3*(code-8)))&((res>0)?1:((res<0)?4:2)))>0
          ?Boolean.TRUE:Boolean.FALSE;
      } else {  // binary on primitive types
        // Widen
        Number n1=widen(c1w,c1ID);
        Number n2=widen(c2w,c2ID);
      
        // Perform      
        boolean boolres=false;
        boolean resbool=false;
        if ((opsIDX>=6) && (opsIDX<=7)) {  // operations on floating point
          double d1=n1.doubleValue(),d2=n2.doubleValue();
          boolean wrop=false;
          switch (code) {
          case 0 : d1=d1+d2; break; //PL
          case 1 : d1=d1-d2; break; //MI
          case 2 : d1=d1*d2; break; //MU
          case 3 : d1=d1/d2; break; //DI
          case 4 : d1=d1%d2; break; //RE
          case 5 : wrop=true;break; //AN
          case 6 : wrop=true;break; //OR
          case 7 : wrop=true;break; //XO
          case 8 : boolres=true; resbool=(d1==d2); break; //EQ
          case 9 : boolres=true; resbool=(d1!=d2); break; //NE
          case 10: boolres=true; resbool=(d1<d2);  break; //LT
          case 11: boolres=true; resbool=(d1>=d2); break; //GE
          case 12: boolres=true; resbool=(d1>d2);  break; //GT
          case 13: boolres=true; resbool=(d1<=d2); break; //LE
          default :
            wrop=true;
          };
          if (Debug.enabled && wrop)
            Debug.println("Wrong operation on float ("+code+").");      
        
          if (!boolres) n1=new Double(d1);
          else { // booleans are represented by longs temporarily
            if (resbool) n1=new Long(1L); else n1=new Long(0);
          };
        } else { // operations on integers
          long l1=n1.longValue(),l2=n2.longValue();
          switch (code) {
          case 0: l1=l1+l2;break; //PL
          case 1: l1=l1-l2;break; //MI
          case 2: l1=l1*l2; break; //MU
          case 3: l1=l1/l2; break; //DI
          case 4: l1=l1%l2; break; //RE
          case 17:
          case 5: l1=l1&l2; break; //AN
          case 18:
          case 6: l1=l1|l2; break; //OR
          case 7: l1=l1^l2; break; //XO
          case 8 : boolres=true; l1=(l1==l2?1L:0L); break; //EQ
          case 9 : boolres=true; l1=(l1!=l2?1L:0L); break; //NE
          case 10: boolres=true; l1=(l1< l2?1L:0L); break; //LT
          case 11: boolres=true; l1=(l1>=l2?1L:0L); break; //GE
          case 12: boolres=true; l1=(l1> l2?1L:0L); break; //GT
          case 13: boolres=true; l1=(l1<=l2?1L:0L); break; //LE
          case 14: l1=l1<<l2; break;  // LS
          case 15: l1=l1>>l2; break;  // RS
          case 16: { // for this kind of shifts the bit width of variable is
            // important
            if (resID==4) //=because there is unary numeric promotion before op
              l1=(int)l1>>>l2; 
            else 
              l1=l1>>>l2; 
            break; 
          } // RUS
          default :
            if (Debug.enabled)
              Debug.println("Wrong operation on integer ("+code+").");      
          };
          n1=new Long(l1);
        };
      
        // Narrow    
        if (boolres) {
          c1w=narrow(n1,0);
        } else c1w=narrow(n1,resID);
        
      };
      return c1w;
    } catch (Exception thr) {
      // if can't evaluate -- replace operands at least

      if (!ep1)
        chi[0]=new OPload(chi[0],c1w);
      
      if (!ep2)
        chi[1]=new OPload(chi[1],c2w);

      throw thr;
      //      Debug.reportThrowable(thr);
      // IGNORE
    }
  };

};

